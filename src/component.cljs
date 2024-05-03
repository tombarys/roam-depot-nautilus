(ns nautilus-roam-3-5-2024-v5d
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [roam.datascript :as rd]
            [roam.block :as block]
            [roam.datascript.reactive :as rdr]))

;; ------- default settings -------

(def init-duration 15) ;; value used when no duration is specified as a render parameter

(def init-len-limit 22) ;; value used when no legend length limit is specified as a render parameter

(def custom-color-1 "rgba(255,0,0,0.5)")

(def init-custom-color-1-tag "")

(def init-workday-start 480)

;; ------- other defaults –––––––

(def workday-end 1320)

(def init-starting-distance 30)

;; ------ legend placement vs performance ----------

(def tries-treshold 25) ;; number of legend placement guesses; lower number = faster but more likely to overlap

;; -------------- scaling ---------------

(defonce mobile? js/window.roamAlphaAPI.platform.isMobile)

(def start-svg-rect-ratio 0.7)

(defonce snail-scaler (if mobile? 0.7 1)) ;; changes the size of the snail (and thus proportions of the whole chart)

(def mob-width 450) ;; default start width value on mobile

(def desk-width 600) ;; default start width value on desktop

;; ---------- mostly visual dev settings ------------

(def shaky false) ;; beta feature

(def reserve 15) ;; reserve space left and right

(def bent-line-gap 3) ;; the space between the bent line and the legend rectangle

(def rect-width-coef 1.55) ;; bigger number = narrower text rect (for legend)

(def rect-height-coef 1.15) ;; bigger number = taller text rect (for legend)

(def font-family "Inter, sans-serif")

(def font-size (if mobile? 12 14))

(def snail-blueprint-outer-radiuses
  (concat (repeat 5 0) [135 140 145 150] (range 145 65 -5)))

(def snail-inner-radius (* 50 snail-scaler))

(defn outer-radius-at [t]
  (* (nth snail-blueprint-outer-radiuses t) snail-scaler))

(def len-central-legend 16) ;; length of the central legend description (page name or date)

;; ----------------- colors, darling ---------------

(def snail-template-color "#888888")

(def clock-hand-color "#EA0F0F5B")

(def meeting-color-palette
  ["rgba(252,194,0,0.8)", "rgba(252,194,0,0.6)", "rgba(252,194,0,0.4)", "rgba(252,194,0,0.3)"])

(def todo-color-palette
  ["rgba(4,100,132,0.3)", "rgba(8,153,200,0.3)", "rgba(47,186,232,0.3)", "rgba(58,202,249,0.3)"])

;; -------------- debug support ------------ 

(def debug-state-atom (r/atom false))        
                                             
(defn safe-prn [s]
    (clojure.pprint/pprint s)
    s)                                      

(defn safe-prn-debug? [s]                           
  (when @debug-state-atom                    
    (clojure.pprint/pprint s)                
    s))                                      
                                             
(defn println?debug [& args]                 
  (when @debug-state-atom                    
    (apply println args)))                   
                                             
(defn pprint-all [& args]                    
  (clojure.pprint/pprint (apply str args)))  
                                             
(defn pprint?debug [& args]                      
  (when @debug-state-atom                        
    (clojure.pprint/pprint (apply str args))))   
                                             
(defn draw-debug-rects [rects]                
  [:g (for [{:keys [w h x y]} rects]          
        [:g                                   
         [:rect                               
          {:style {:fill "rgba(128,128,128,0.32)"}    
           :x x                
           :y y                
           :width w            
           :height h}]])])     

;; ---------- resolving (()) references -----------

(defn str-with-resolved-block-refs [{:keys [block/string block/refs]}]
  (reduce (fn [string ref-ent]
            (str/replace string (str "((" (:block/uid ref-ent) "))") (:block/string ref-ent)))
          string
          refs))

;; --------- math is beautiful ---------

(def pi js/Math.PI)

(defn abs [x] (js/Math.abs x))

(defn cos [x] (js/Math.cos x))

(defn sin [x] (js/Math.sin x))

(defn round2 [num]
  (-> (* num 100)
      (js/Math.round)
      (/ 100.0)))

(defn angle->rad [angle]
  (* (- 180 angle) (/ pi 180)))

(defn pos-sweep-angle
  "Correctly calculates the angular range"
  [start-radians end-radians]
  (- (* 2 pi) (if (> end-radians start-radians)
                (- end-radians start-radians)
                (+ (- end-radians start-radians) (* 2 pi)))))

(defn pos-sweep-angle-mid
  "Correctly calculates the middle of the angular range"
  [start-radians end-radians]
  (+ end-radians (/ (pos-sweep-angle start-radians end-radians) 2)))

(defn min->angle [minutes]
  (mod (/ (- minutes 540) 2) 360))

;; --------------- legend collision solution -----------

(defn between [x a b]
  (and (>= x a) (<= x b)))

(defn collide? [new-rect any-rect]
  (let [ntlx (:x new-rect)
        ntly (:y new-rect)
        nbrx (+ ntlx (:w new-rect))
        nbry (+ ntly (:h new-rect))
        tlx (:x any-rect)
        tly (:y any-rect)
        brx (+ tlx (:w any-rect))
        bry (+ tly (:h any-rect))]
    (not (or (< nbrx tlx)
             (> ntlx brx)
             (< nbry tly)
             (> ntly bry)))))

(defn collides?
  "Tests if new-rect overlaps with any of the rects"
  [new-rect rects]
  (boolean (some #(collide? new-rect %) rects)))

(defn at-vertex [radians]
  (or (between radians 1.01 2.05) (between radians -2.05 -1.01)))

(defn iterate-rect-place
  "returns the new coordinates of new-rect that does not overlap with any of the rects;
   radians - the angle at which new-rect first tries to position itself;
   radius - the distance from the center;
   text – the legend text that is written to the debug console 
   radians-span - the maximum angular deviation"
     
  [new-rect rects start-radians start-radius text center]
  (let [max-legend-radius (* start-radius 1.7) ;; maximum legend radius to try
        max-radians-span (/ pi 17)] ;; maximum angle span for legend to try
    (loop [radians start-radians
           radius start-radius
           angle-offset  0
           radius-offset 0
           counter 0
           radius-inc 3 ;; radius offset step size
           trying (if (at-vertex radians) :radius :angle)]
      (let [min-radians (- radians (/ max-radians-span 2))
            max-radians (+ radians (/ max-radians-span 2))
          ; min-radius (- start-radius 10) 
            x (+ (:center-x center) (* (cos radians) radius))
            y (+ (:center-y center) (* (sin radians) radius))
            on-left? (or (> radians (/ pi 2)) (< radians (- (/ pi 2))))
            at-vertex? (at-vertex radians)
            horizontal-shift (if at-vertex?
                               (/ (:w new-rect) 2)
                               (if on-left?
                                 (:w new-rect)
                                 0))
            vertical-shift (/ (:h new-rect) 2)
            new-rect (assoc new-rect :x (- x horizontal-shift) :y (- y vertical-shift) :radians radians)
            colliding? (collides? new-rect rects)]
        (pprint?debug text " TRYING: " trying " x: " (round2 x) " y: " (round2 y) 
                      " radius: " (round2 radius) " radians: " (round2 radians)   
                      " radius-offset " radius-offset                             
                      " angle-offset: " (round2 angle-offset)                     
                      " colliding?: " colliding? " counter " counter)             
        (if (or (> counter tries-treshold) (not colliding?)) ;; number of placement guesses; lower number = faster but more likely to overlap
          new-rect
          (if (= trying :radius) ;; testing placing using increased radius 
            (if (< radius max-legend-radius)
              (recur radians
                     (+ start-radius radius-offset)
                     angle-offset
                     (+ radius-offset radius-inc)
                     (inc counter)
                     radius-inc
                     :radius)
              (recur radians start-radius angle-offset radius-offset 0 0 :angle))
            (if (and (> radians min-radians) (< radians max-radians)) ;; testing increased/decreased angle 
              (recur (+ start-radians angle-offset)
                     radius
                     (if (or (= 0 angle-offset) (pos? angle-offset))
                       (- (+ 0.03 angle-offset)) ;; angle offset step size
                       (+ 0.03 (- angle-offset)))
                     radius-offset
                     (inc counter)
                     0
                     :angle)
              (recur start-radians start-radius angle-offset radius-offset 0 radius-inc :radius))))))))

(defn real-rect-radians [rect center]
  (let [rcenter-x (+ (:x rect) (/ (:w rect) 2))
        rcenter-y (+ (:y rect) (/ (:h rect) 2))
        center-x (:center-x center)
        center-y (:center-y center)]
    (js/Math.atan2 (- rcenter-y center-y) (- rcenter-x center-x))))

(defn get-legend-rect
  "Returns a new legend rectangle that does not overlap with any of the rects"
  [rects text slice-radians outer-radius center settings]
  (let [w (* (/ font-size rect-width-coef) (min (count text) (:legend-len-limit settings)))  
        h (* font-size rect-height-coef)                          
        new-text-rect (if rects
                        (assoc
                         (iterate-rect-place {:w w :h h}               
                                             rects                     
                                             (- slice-radians)         
                                             (+ outer-radius (if mobile? 0 init-starting-distance))  ;; starting distance from the centre
                                             text
                                             center)
                         :text text)                                   ;; FIXME jen pro debug              
                        {})]
    (assoc new-text-rect :real-rect-radians (real-rect-radians new-text-rect center))))

;; --------------- o`clock ----------------------

(defonce now-time-atom (r/atom 0))

(defonce minute-updater
  (js/setInterval
   #(let [now (new js/Date (.now js/Date))]
      (reset! now-time-atom (+ (* (.getHours now) 60)
                               (.getMinutes now))))
   10000))



;; --------------- reading / writing Roam database ----------------------

(defn eval-state [*get-children]
  (:block/children @*get-children))

(defn get-children-strings [block-uid]
  (r/with-let [*get-children-atom (rdr/pull
                                   [{:block/children [:block/string :block/order {:block/refs [:block/string :block/uid]}]}]
                                   [:block/uid block-uid])
               *children (r/track eval-state *get-children-atom)]
    (map str-with-resolved-block-refs
         (->> @*children
              (sort-by :block/order)))))

(defn get-page-title [page-uid] ;; when you have a block-uid for a page
  (-> (rd/q '[:find ?title
              :in $ ?page-uid
              :where [?e :block/uid ?page-uid]
              [?e :node/title ?title]]
            page-uid)
      first
      first))

(defn page-title [block-uid]
  (str (rd/q
        '[:find ?parent-page-title .
          :in $ ?block-uid
          :where
          [?block :block/uid  ?block-uid]
          [?block :block/page ?page]
          [?page  :node/title ?parent-page-title]]
        block-uid)))

(defn daily-page? [block-uid]
  (if (= (page-title block-uid)
         (js/window.roamAlphaAPI.util.dateToPageTitle (new js/Date (.now js/Date))))
    true
    false))

(defn get-block-str-naked [block-uid]
  (-> (rd/q '[:find ?s
              :in $ ?uid
              :where
              [?b :block/uid ?uid]
              [?b :block/string ?s]]
            block-uid)
      first
      first))

(defn minutes->time [minutes]
  (let [h (int (/ minutes 60))
        m (mod minutes 60)]
    (str (if (< h 10) (str "0" h) h) ":" (if (< m 10) (str "0" m) m))))

(defn rm-prog-from-block-if-done [uid]
  (let [str (str/replace (get-block-str-naked uid) #"\sd\d{1,3}\%" "")]
    (block/update {:block
                   {:uid uid
                    :string str}})))

(defn update-block-progress [block-uid increment] 
   (let [s (get-block-str-naked block-uid)
         progress-format #"(\sd)(\d{1,3})(\%)" ; #"(\s\%)(\d{1,3})"
         updated-str (if-let [progress-match (re-find progress-format s)]
                       (let [old-progress-str (first progress-match)
                             prog-incremented (+ (int (last (butlast progress-match))) increment)
                             prog-new-str (cond
                                            (= prog-incremented 100) "done"
                                            (> prog-incremented 100) ""
                                            :else (str " d" prog-incremented "%"))]
                         (if (not= prog-new-str "done")
                           (str/replace s old-progress-str prog-new-str)
                           (str (->
                                 (str/replace s old-progress-str "")
                                 (str/replace #"\{\{\[\[TODO\]\]\}\}" "{{[[DONE]]}}"))
                                " d" (minutes->time @now-time-atom))))
                       (-> (str s " d" increment "%")
                           (str/replace #"\{\{\[\[DONE\]\]\}\}" "{{[[TODO]]}}")
                           (str/replace #"\b(d\d{1,2}(:\d{1,2})?)\b(?!%)" "")))]
     (block/update {:block
                    {:uid block-uid
                     :string updated-str}})))


;; ---------------- helpers ----------------------

(defn update-opacity-str [color opacity]
  (let [s (subs color 0 (- (count color) 5))
        new-color (str s "," opacity ")")]
    new-color))

(defn shake-if [shaky]
  (if shaky (- (rand-int 4) 2) 0))


;; --------------- text parsers --------------------



(defn from-1224->min [time-str h12] ;; if h12 is "am"/"pm" ; h12 = nil => h24
  (let [pm? (re-find #"(?:pm|PM)" time-str)
        am?  (re-find #"(?:am|AM)" time-str)
        new-time-str (->
                      (str/replace time-str #"(?:am|AM|pm|PM)" "")
                      (str/trim))
        [hours new-mins] (if (re-find #"\:" new-time-str)
                           (mapv int (str/split new-time-str #":"))
                           [(int new-time-str) 0])
        new-hours (if (and (not am?) (or pm? (and h12 (= (clojure.string/lower-case h12) "pm"))))
                    (if (= hours 12) 12 (+ hours 12))
                    (if am?
                      (if (= hours 12) 0 hours)
                      hours))
        [h m] [(mod new-hours 24) (mod new-mins 60)]]
    [(+ m (* 60 h)) (or am? pm?)]))

(defn parse-time-range [s]
  (let [range-format #"(?:\d{1,2}(?::\d{1,2})?(?:\s*(?:\s?AM|\s?PM|\s?am|\s?pm))?)\s*(?:-|–|až|to)\s*(?:\d{1,2}(?::\d{1,2})?(?:\s*(?:\s?AM|\s?PM|\s?am|\s?pm))?)"
        range-str (re-find range-format s)]
    (if range-str
      (let [cleaned-str (-> (str/replace s range-str "")
                            (str/replace #"\s\s" " "))
            [_ from-str to-str] (re-find #"(.*)(?:-|–|až|to)(.*)" range-str)
            [to-min h12] (from-1224->min to-str nil)
            [from-min _] (from-1224->min from-str h12)]
        {:range (if (> to-min from-min)
                  [from-min to-min]
                  [from-min from-min])
         :cleaned-str cleaned-str})
      {:range nil :cleaned-str s})))

(defn parse-duration [s settings]
  (let [duration-format #"(\d{1,3})(min|m)"]
    (if-let [duration-match (re-find duration-format s)]
      (let [duration-str (first duration-match)
            cleaned-str (str/replace s duration-str "")]
        {:duration (int (second duration-match))
         :cleaned-str cleaned-str})
      {:duration (:default-duration settings) :cleaned-str s})))

(defn parse-progress [s]
  (let [progress-format #"(\sd)(\d{1,3})(\%)"]
    (if-let [progress-match (re-find progress-format s)]
      (let [progress-str (first progress-match)
            cleaned-str (str/replace s progress-str "")
            prog-int (int (last (butlast progress-match)))]
        {:progress (if (> prog-int 100) 100 prog-int)
         :cleaned-str cleaned-str})
      {:progress 0 :cleaned-str s})))

(defn parse-done-time [s]
  (let [done-time-format #"d(\d{1,2}(?::\d{1,2})?)"
        done-time-match (re-find done-time-format s)]
    (if done-time-match
      (let [[_ done-time-str] done-time-match
            [h m] (str/split done-time-str #":")
            cleaned-str (str/replace s (str "d" done-time-str) "")]
        {:done-at (+ (if m (int m) 0) (* 60 (int h)))
         :cleaned-str cleaned-str})
      {:done-at nil :cleaned-str s})))

(defn parse-DONE [s]
  (let [done-format #"\{\{\[\[DONE\]\]\}\}"
        done-found? (re-find done-format s)]
    (if done-found?
      {:done true
       :cleaned-str (-> s
                     (str/replace done-format "")
                     (str/replace #"\s\%\d{1,3}" ""))}
      {:done false :cleaned-str s})))

(defn parse-custom-color-1 [s {:keys [custom-color-1-tag]}]
  (let [color-format (re-pattern (str "(?<=^|\\s)" custom-color-1-tag "(?=$|\\s)")) 
        color-found? (re-find color-format s)]
    (if (and (seq custom-color-1-tag) color-found?)
      {:custom-color custom-color-1
       :cleaned-str s}
      {:custom-color nil 
       :cleaned-str s})))

(defn parse-URLs
  "Extract and format URL links"
  [s]
  (str/replace s #"\[([^\]]*?)\]\((.*?)\)" "$1"))

(defn parse-rest [s]
  (-> s
      ;; Remove specific Roam markers (TODO, DONE, etc.)
      (str/replace #"\{\{\[\[TODO\]\]\}\}" "")
      (str/replace #"\{\{\[\[DONE\]\]\}\}" "")

      ;; Remove wiki links
      (str/replace #"\[\[(.*?)\]\]" "$1")

      ;; Remove other special formatting (bold, italic, etc.)
      (str/replace #"\*\*(.*?)\*\*" "$1")
      (str/replace #"\_\_(.*?)\_\_" "$1")
      (str/replace #"\^\^(.*?)\^\^" "$1")

      ;; Remove embeds (idiot version, but works)
      (str/replace #"\{\{(\[\[)?embed(\]\])?\:" "")
      (str/replace #"\}\}" "")


      ;; Trim double spaces and whitespace
      (str/replace #"---" "")
      (str/replace #"\s\s" " ")
      (str/trim)))

(defn parse-row-params [block-map settings]
  (let [s (:s block-map)
        cleaned-str (parse-URLs s) ;; remove URLs – it has to start with this, because URLs can contain other markers
        {:keys [custom-color cleaned-str]} (parse-custom-color-1 cleaned-str settings)
        {:keys [range cleaned-str]} (parse-time-range cleaned-str)
        {:keys [duration cleaned-str]} (parse-duration cleaned-str settings)
        {:keys [progress cleaned-str]} (parse-progress cleaned-str)
        {:keys [done-at cleaned-str]} (parse-done-time cleaned-str)
        {:keys [done cleaned-str]} (parse-DONE cleaned-str)
        description (parse-rest cleaned-str)
        event-type (if range :meeting :todo)]
    (when done
      (rm-prog-from-block-if-done (:uid block-map)))
    (-> {:description description
         :progress progress
         :duration (int (* (/ (- 100 progress) 100) duration))
         :uid (:uid block-map)
         :start (if done-at (abs (- done-at duration)) (first range))
         :end (or done-at (second range))
         :done done
         :bg-color custom-color
         :done-at (if done done-at nil)}
        (assoc event-type true))))

;; --------------- fill day with events and todos ----------------------

(defn fill-day [events workday-start plan-from-time]
  (let [sorted-events (sort-by #(if (:meeting %) (:start %) 0) events)
        todo-events (filter #(= true (:todo %)) sorted-events)
        meeting-events (filter #(= true (:meeting %)) sorted-events)
        not-done-todos (filter #(not (:done %)) todo-events)]
    (loop [todos not-done-todos
           meetings meeting-events
           time workday-start
           result []]
      (if (and (empty? todos) (empty? meetings))
        (conj result {:freetime true :start time :end workday-end})  ; Day is over (no todos or meetings left)
        (if (< time plan-from-time)
          (let [time (max time workday-start)
                next-meeting (first meetings)]
            (if next-meeting
              (if (> (:start next-meeting) time)
                (recur todos meetings (min plan-from-time (:start next-meeting)) (conj result {:freetime true :start time :end (min plan-from-time (:start next-meeting))}))
                (recur todos (rest meetings) (:end next-meeting) (conj result next-meeting)))
              (recur todos [] plan-from-time (conj result {:freetime true :start time :end plan-from-time}))))
          (let [time (max time workday-start) ; Ensure time doesn't fall before workday-start
                next-todo (first todos)
                next-meeting (first meetings)]
            (if next-meeting
              (if (> (:start next-meeting) time)
                ;; if the next meeting starts later than the current time
                (if (and next-todo (< (+ time (:duration next-todo)) (:start next-meeting)) (<= (:start-after next-todo) time))
                    ;; if there is another todo and it ends before the next meeting starts,
                    ;; and at the same time starts after start-after (i.e. the end of the previous meeting after which it was placed in the list)
                  (recur (rest todos) meetings (+ time (:duration next-todo)) (conj result (assoc next-todo :start time :end (+ time (:duration next-todo)))))
                  (recur todos meetings (:start next-meeting) (conj result {:freetime true :start time :end (:start next-meeting)})))
                (recur todos (rest meetings) (:end next-meeting) (conj result next-meeting)))
              (if next-todo
                (recur (rest todos) [] (+ time (:duration next-todo)) (conj result (assoc next-todo :start time :end (+ time (:duration next-todo)))))
                (recur [] [] time result)))))))))


;; --------------- slice component ----------------------

(defn bent-line-component
  [legend-start-x legend-start-y text-x text-y color]
  (let [new-text-x text-x
        new-text-y (+ 3 text-y)
        middle-legend-text-x (/ (+ legend-start-x new-text-x) 2)
        middle-legend-text-y (+ (/ (+ legend-start-y new-text-y) 2) 10)] ; increase 30 to make line more bent
    [:g
     [:path {:d (str "M " legend-start-x "," legend-start-y " Q "
                     middle-legend-text-x "," middle-legend-text-y " "
                     new-text-x "," new-text-y)
             :stroke color
             :stroke-width "1px"
             :fill "transparent"
             :stroke-dasharray "2,1"}]]))

(defn calculate-coordinates
  "Calculates the x and y coordinates based on angle, radius, and center position."
  [angle radius center]
  (let [radians (angle->rad angle)]
    [(+ (:center-x center) (* (cos radians) radius))
     (- (:center-y center) (* (sin radians) radius))]))

(defn create-arc-path
  "Constructs the SVG path for the arc based on start and end angles and radii."
  [start-angle end-angle inner-radius outer-radius center]
  (let [start-radians (angle->rad start-angle)
        end-radians (angle->rad end-angle)
        start-coord-outer (calculate-coordinates start-angle outer-radius center)
        end-coord-outer (calculate-coordinates end-angle outer-radius center)
        start-coord-inner (calculate-coordinates start-angle inner-radius center)
        end-coord-inner (calculate-coordinates end-angle inner-radius center)
        large-arc-flag (if (>= (pos-sweep-angle start-radians end-radians) pi) 1 0) #_(if (>= (abs (- end-angle start-angle)) 180) 1 0)]
    (str "M" (first start-coord-outer) "," (second start-coord-outer)
         " A" outer-radius "," outer-radius " 0 " large-arc-flag " 1 " (first end-coord-outer) "," (second end-coord-outer)
         " L" (first end-coord-inner) "," (second end-coord-inner)
         " A" inner-radius "," inner-radius " 0 " large-arc-flag " 0 " (first start-coord-inner) "," (second start-coord-inner)
         "Z")))

(defn slice
  "Draws and colors the slice section according to the specified parameters"
  [[start-angle end-angle inner-radius outer-radius center settings]
   & {:keys [bg-color
             border-color
             legend-rect
             text
             timestamp
             stroke-dasharray ;; border type
             font-weight
             shaky            ;; "unsure hand style" allowed
             done?
             uid              ;; roam block uid
             non-zero-progress?
             click-to-progress ]}] ;; click to progress feature allowed for the slice?
  (let [; hovered (r/atom false)
        start-radians (angle->rad (+ start-angle (shake-if shaky)))
        end-radians (angle->rad (+ end-angle (shake-if shaky)))
        mid-radians (pos-sweep-angle-mid start-radians end-radians)
        inner-radius (+ inner-radius (shake-if shaky))
        outer-radius (+ outer-radius (shake-if shaky))
        [center-x center-y] [(:center-x center) (:center-y center)]
        [legend-line-start-x legend-line-start-y] [(+ (* (cos mid-radians) (+ bent-line-gap outer-radius)) center-x) (- center-y (* (sin mid-radians) (+ outer-radius bent-line-gap)))] ; where the line starts (from the center)
        [legend-x legend-y] [(:x legend-rect) (:y legend-rect)]
        [legend-w legend-h] [(:w legend-rect) (:h legend-rect)]
        legend-radians (- (:real-rect-radians legend-rect))
        at-vertex? (at-vertex legend-radians)
        [legend-line-end-x legend-line-end-y]
        (if at-vertex?
          [(+ legend-x (/ legend-w 2)) (+ legend-y (if (< legend-radians 0) 0 legend-h))] ; pokud je legenda na vrcholu, tak je konec čáry uprostřed legendy
          (cond
            (and (< legend-radians pi) (> legend-radians (/ pi 2))) [(+ legend-x legend-w bent-line-gap) (+ legend-y (* legend-h (sin legend-radians)))] ; levý horní
            (and (< legend-radians (/ pi 2)) (> legend-radians 0)) [legend-x (+ legend-y (/ legend-h 2) (* legend-h (/ (sin legend-radians) 2)))] ; pravý horní
            (and (< legend-radians 0) (> legend-radians (- (/ pi 2)))) [legend-x (+ legend-y (* legend-h (/ (cos legend-radians) 2)))] ; pravý dolní
            :else [(+ legend-x legend-w bent-line-gap) (+ legend-y (* (/ (+ (sin legend-radians) 1) 2) legend-h))])) ; levý dolní
        time-text-x (+ center-x (* (cos start-radians) (- outer-radius 10)))
        time-text-y (- center-y (* (sin start-radians) (- outer-radius 10)))
        border-color (if (= border-color nil) "none" border-color)
        stroke-dasharray (if (= stroke-dasharray nil) "2,2" stroke-dasharray)
        bg-color (if (= bg-color nil) "rgba(255,255,255,0)" bg-color)
        legend-color (if-not done? (update-opacity-str bg-color "1") (update-opacity-str bg-color "0.2"))
        font-weight (if font-weight font-weight "normal")
        path (create-arc-path start-angle end-angle inner-radius outer-radius center)
        debug? @debug-state-atom                                       
        dbg-radians-txt (if debug? (str "slc:" (round2 start-radians)  
                                        "–>" (round2 end-radians) "/ leg:" (round2 legend-radians)) "") 
        on-left? (or (<= legend-radians (- (/ pi 2))) (>= legend-radians (/ pi 2)))] 
    [:g
     [:defs
      [:pattern
       {:id "dot-pattern" :width "4" :height "4" :patternUnits "userSpaceOnUse"}
       [:circle {:r "0.5" :cx "1" :cy "1" :fill "gray"}]
       [:circle {:r "0.5" :cx "5" :cy "5" :fill "gray"}]]]
     (when @debug-state-atom  [:circle {:cx center-x :cy center-y :r 4 :fill "red"}])              
     ;; ⤵ this is the main component - slice
     
     (when non-zero-progress? [:path
      {:d path
       :fill "url(#dot-pattern)"}])
     
     [:path
      {:d path
       :style (when click-to-progress {:cursor "pointer"})
       :stroke-dasharray stroke-dasharray
       :fill bg-color
       :on-click #(when click-to-progress (update-block-progress uid 10))
       ; :on-mouse-enter (fn [_] (reset! hovered true))
       ; :on-mouse-leave (fn [_] (reset! hovered false))
       :stroke border-color}]
     ;; ⤵ adds an event legend
     ;; (when @hovered [:g [:text {:x center-x :y center-y} (str progress)]])
     (when (and text (not done?))
       [:g
        [bent-line-component legend-line-start-x legend-line-start-y legend-line-end-x legend-line-end-y legend-color]
        [:text {:x (+ legend-line-end-x (if on-left? (- bent-line-gap) bent-line-gap)) :y (+ legend-y legend-h)
                :text-anchor (if at-vertex?
                               "middle"
                               (if on-left? "end" "start"))
                :alignment-baseline "baseline"
                :font-weight font-weight
                :style (when click-to-progress {:cursor "pointer"})
                :on-click #(when click-to-progress (update-block-progress uid 10))
                :fill (if-not done? (update-opacity-str bg-color "1") (update-opacity-str bg-color "0.2"))}
         (if debug? (str dbg-radians-txt)                                            
             (str #_progress-str (subs text 0 (:legend-len-limit settings))))        
         ]])     
     (when (seq timestamp)
       ;; ⤵ adds a clock label for the snail template
       [:text  {:x time-text-x :y time-text-y :font-size (- font-size 3) :font-family font-family :color border-color :fill border-color
                :transform (str "rotate(" (if
                                           (or (>= start-angle 270)
                                               (<= start-angle 90))
                                            start-angle
                                            (- start-angle 180)) " " time-text-x "," time-text-y ")")
                :text-anchor "middle"
                :alignment-baseline (if
                                     (or (>= start-angle 270)
                                         (<= start-angle 90))
                                      "after-edge"
                                      "before-edge")}
        (if debug? (str (round2 start-radians) " / " start-angle) timestamp)])]))


(defn snail-blueprint-component [color inner-radius center settings]
  (let
   [workday-start (:workday-start settings)]
    [:g (mapcat (fn [[start end timestamp]]
                  [[slice
                    [start end inner-radius (outer-radius-at timestamp) center settings]
                    :border-color color
                    :timestamp (str timestamp)]])
                (concat
                 (cond (between workday-start 420 479) [(vector 300 330 7)]
                       (between workday-start 360 419) [(vector 270 300 6) (vector 300 330 7)])
                 (map vector
                      (range 0 390 30)
                      (range 30 390 30)
                      (range 9 21))
                 [(vector 0 30 21) (vector 330 360 8)]))]))

(defn central-label-component [[first-row second-row] center]
  (let [[center-x center-y] [(:center-x center) (:center-y center)]
        common-attr {:x center-x
                     :fill "gray"
                     :alignment-baseline "middle"
                     :text-anchor "middle"
                     :font-weight "bold"
                     :font-size (str (* font-size 4/5))}]
    [:g
     [:text (assoc common-attr :y (- center-y (/ font-size 2))) first-row]
     [:text (assoc common-attr :y (+ center-y (/ font-size 2))) second-row]]))

(defn calculate-slice-params [event index daily-page?]
  (let [outer-radius (outer-radius-at (mod (quot (int (:start event)) 60) (count snail-blueprint-outer-radiuses)))
        start-angle (min->angle (:start event))
        end-angle (min->angle (:end event))
        todo? (:todo event)
        done-at (:done-at event)
        done? (if (:done event) true false)
        meeting? (:meeting event)
        progress (:progress event)
        click-to-progress (if (and daily-page? todo?) true false)
        expired? (and meeting? (>= @now-time-atom (:end event)))
        todo-bg-color (or (:bg-color event ) (nth todo-color-palette (mod index (count todo-color-palette))))
        meeting-color (or (:bg-color event) (nth meeting-color-palette (mod index (count meeting-color-palette))))]
    {:start-angle start-angle
     :end-angle end-angle
     :bg-color (cond
                 meeting? (if (and daily-page? expired?) "rgba(128,128,128,0.1)" meeting-color)
                 todo? (if done-at "rgba(128,128,128,0.1)" todo-bg-color)
                 :else nil)
     :done done?
     :outer-radius outer-radius
     :progress progress
     :click-to-progress click-to-progress}))

(defn event-slice-component [event index legend-rect inner-radius daily-page? center settings]
  (let [{:keys [start-angle end-angle bg-color done outer-radius click-to-progress]} (calculate-slice-params event index daily-page?)
        description (:description event)
        uid (:uid event)
        progress (:progress event)]
    [slice
     [start-angle end-angle inner-radius outer-radius center settings]
     :bg-color bg-color
     :text description
     :shaky shaky
     :done? done
     :uid uid
     :progress progress
     :font-weight "bold"
     :legend-rect legend-rect
     :progress progress
     :non-zero-progress? (if (> progress 0) true false)
     :click-to-progress click-to-progress]))

(defn events->slices
  "Returns svg vector of all slice components + list of legend rectangles"
  [events daily-page-atom? center settings]
  (loop [i 0
         events (filter #(not= true (:freetime %)) events)
         rects []
         all-slice-components [:g]]
    (if-let [event (first events)]
      (let [mid-radians (pos-sweep-angle-mid
                         (angle->rad (min->angle (:start event)))
                         (angle->rad (min->angle (:end event))))
            text (:description event)
            radius (nth snail-blueprint-outer-radiuses (mod (quot (int (:start event)) 60) (count snail-blueprint-outer-radiuses)))
            new-rect (get-legend-rect rects text mid-radians radius center settings)]
        (println?debug "RADIUS INSIDE EVENTS-SLICES: " radius)              
        (recur (inc i) (rest events) (conj rects new-rect) (conj all-slice-components (event-slice-component event i new-rect snail-inner-radius @daily-page-atom? center settings))))
      [all-slice-components rects])))

(defn events->new-dimensions
  "Returns a new center and width so that events can be aligned"
  [events center settings]
  (let [center-x (:center-x center)
        center-y (:center-y center)]
    (loop [i 0
           events (filter #(not= true (:freetime %)) events)
           rects []
           left-min (- center-x (outer-radius-at 9))
           right-max (+ center-x (outer-radius-at 14))
           top-min (- center-y (outer-radius-at 11))
           bottom-max (+ center-y (outer-radius-at 17))]
      (if-let [event (first events)]
        (let [mid-radians (pos-sweep-angle-mid
                           (angle->rad (min->angle (:start event)))
                           (angle->rad (min->angle (:end event))))
              text (:description event)
              radius (nth snail-blueprint-outer-radiuses (mod (quot (int (:start event)) 60) (count snail-blueprint-outer-radiuses)))
              new-rect (get-legend-rect rects text mid-radians radius center settings)]
          (recur (inc i) (rest events) (conj rects new-rect) (min left-min (:x new-rect)) (max right-max (+ (:x new-rect) (:w new-rect))) (min top-min (:y new-rect)) (max bottom-max (+ (:y new-rect) (:h new-rect)))))
        [(+ reserve (- center-x left-min))
         (+ reserve (- right-max left-min))
         (+ reserve (- center-y top-min))
         (+ (* 3 reserve) (- bottom-max top-min) (when (< (:workday-start settings) 420) reserve))])))) ;; when the workday starts before 7:00, the snail has to get more space below

(defn split-and-trim [page-title n]
  (map #(subs % 0 (min n (count %))) (str/split page-title #"," 2)))

(defn show-events [events-state daily-page-atom? show-done-atom? page-title dimensions settings]
  (let [[events done-todos] @events-state
        old-width (js/Math.round (:width dimensions))
        old-height (js/Math.round (:height dimensions))
        [center-x suggested-width center-y suggested-height] (events->new-dimensions events {:center-x (/ old-width 2) :center-y (/ old-height 2)} settings)
        center {:center-x center-x :center-y center-y}
        [all-slice-components rects] (events->slices events daily-page-atom? center settings)]   ;; rects jsou tam jen kvůli debugu, jinak vrací svg vektor
    [:svg {:width (str suggested-width) :height (str suggested-height)
           :xmlns "http://www.w3.org/2000/svg"
           :font-family font-family
           :font-size font-size}
     [:g
      [snail-blueprint-component snail-template-color snail-inner-radius center settings]
      (when  @show-done-atom? ;; má-li ukazovat hotové úkoly, tak je ukáže v šedé
        (map (fn [event] [event-slice-component event 1 nil snail-inner-radius nil center settings]) done-todos))
      all-slice-components         ;; zobrazení všech událostí
      (when @daily-page-atom?      ;; ukáže aktuální čas pomocí úzké výseče
        [slice [(- (min->angle @now-time-atom) 1) (+ (min->angle @now-time-atom) 1) 0 center-y center settings] :bg-color clock-hand-color])
      [central-label-component (split-and-trim page-title len-central-legend) center]
     
      (when @debug-state-atom ;; just for debug ⤵  #FIXME remove in production later
        [:g                                                             
         [draw-debug-rects rects]                                       
         [:text {:x "0" :y "450" :text-anchor "start"}                  
          "Suggested w: " suggested-width                               
          " Center-x: " (:center-x center)                              
          " Center-y: " (js/Math.round center-y)]                       
         [:circle {:cx (:center-x center) :cy (:center-y center)        
                   :r 200 :fill "none" :stroke "black" :stroke-width 1}]])]]))

(defn add-start-after
  "Adds an end time to events so that tasks placed after the meeting cannot start before it"
  [events]
  (loop [events events
         start-after 0
         result []]
    (if (empty? events)
      result
      (let [event (first events)
            new-start     (if (:meeting event)
                            (:end event)
                            start-after)
            updated-event (if (:todo event)
                            (assoc event :start-after new-start)
                            event)]
        (recur (rest events) new-start (conj result updated-event))))))

(defn get-children-maps [block-uid]
  (r/with-let [*get-children-atom (rdr/pull
                                   [{:block/children [:block/uid :block/string :block/order {:block/refs [:block/string :block/uid]}]}]
                                   [:block/uid block-uid])
               *children (r/track eval-state *get-children-atom)]
    (map #(hash-map :s (str-with-resolved-block-refs %) :uid (:block/uid %)) ;; returns vector of maps with keys :s and :uid
         (->> @*children
              (filter #(not= "" (:block/string %)))
              (sort-by :block/order)))))

(defn populate-events
  "Returns vector [events, done_with_done-at]"
  [block-uid plan-from-time settings]
  (let [text->events (-> (mapv #(parse-row-params % settings) (get-children-maps block-uid))
                         (as-> coll
                               (filterv #(not= "" (:description %)) coll))
                         (add-start-after))
        filled-day [(-> text->events
                        (fill-day (:workday-start settings) plan-from-time)) (filter #(:done-at %) text->events)]]
    filled-day))

;; structure of filled-day: [[events] [done-todos]]
;; structure of events: [{:description "text" :duration 30 :start 0 :end 30 :done? false :done-at nil :start-after 0 :todo true :meeting false :freetime false :progress 0}] }]

(defn reset-now-time-atom [now-time-atom]
  (reset! now-time-atom
          (let [now (new js/Date (.now js/Date))
                minutes (+ (* (.getHours now) 60) (.getMinutes now))]
            minutes)))

(defn switch-done-visibility-button [show-done-state]
  [:button
   {:on-click #(swap! show-done-state not)
    :style {:background-color "gray",
            :opacity "50%"
            :color "rgb(254,254,254)"
            :margin "8px"
            :border-radius "8px"
            :display "inline-block"
            :transition "all 0.3s ease"}}
   (str "visible: "
        (if @show-done-state
          "all"
          "undone"))])

(defn switch-debug-button [] ;; debug button
  [:button {:on-click #(swap! debug-state-atom not)    
            :style {:background-color "#5B5B5BBF", :color "rgb(254,254,254)"           
                    :margin "8px" :border-radius "8px" :display "inline-block"}}       
   (if @debug-state-atom                               
     (str "debug is on")                               
     (str "🪲 debug is off"))])                        

(defn arg-tag->str [arg]
  (if (vector? arg)
    (let [uid (second arg)
          decoded (get-page-title uid)]
      (str "#" decoded))
    arg))

(defn args->settings [[a1 a2 a3 a4]]
  (let [a1 (when a1 (int a1))
        a2 (when a2 (int a2))
        a3 (when a3 (int a3))]
      {:legend-len-limit (if (and a1 (between a1 15 30)) a1 init-len-limit) ;; allowed legend length interval
       :default-duration (if (and a2 (between a2 5 60)) a2 init-duration) ;; allowed default todo duration interval
       :workday-start (if (and a3 (between a3 6 8)) (* 60 a3) init-workday-start) ;; allowed default start of the workday 
       :custom-color-1-tag (if (nil? a4)
                             init-custom-color-1-tag
                             (if (string? a4) 
                               (str a4)
                               (arg-tag->str a4)))}))

(defn main [{:keys [:block-uid]} & args]
  (r/with-let [is-running?    #(try
                                  (.-running js/window.nautilusExtensionData)
                                 (catch :default _e
                                   false))
               *running?      (r/atom (or (is-running?)
                                          ;; if not running, we set to nil so that "Loading nautilius extension ..." is shown at first
                                          nil))
               check-interval (js/setInterval #(reset! *running? (is-running?))
                                              ;; check if the value has changed every 5 seconds
                                              5000)]
    (case @*running?
      nil
      [:div
       [:strong "Loading nautilus extension..."]]
      false
      [:div
       [:strong {:style {:color "red"}} "Extension not installed. To use, please install “Nautilus” from Roam Depot."]]
      (do
        (reset-now-time-atom now-time-atom)
        (let [dimensions {:width (if mobile? mob-width desk-width)
                          :height (* start-svg-rect-ratio (if mobile? mob-width desk-width))}
              show-debug-button? (= :debug (first args))
              settings (args->settings args)
              ;; _ (println settings)
              show-done-state (r/atom true)
              daily-page-atom? (r/atom (daily-page? block-uid))
              page-title (page-title block-uid)
              plan-from-time (if @daily-page-atom? @now-time-atom (:workday-start settings))
              events-state (r/atom (populate-events block-uid plan-from-time settings))]
          [:div
           (if-not (nil? @events-state)
             [show-events events-state daily-page-atom? show-done-state page-title dimensions settings]
             (reset! events-state (populate-events block-uid plan-from-time settings)))
           [:div
            [switch-done-visibility-button show-done-state]
            (when show-debug-button? [switch-debug-button])]]))) 
    (finally
      (js/clearInterval check-interval))))