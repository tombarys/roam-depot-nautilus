(ns roam-render-nautilus-v1.0
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [roam.datascript :as rd]
            [roam.datascript.reactive :as rdr]))

;; ------ bfu settings ------

(def pre-default-duration 15)

(def pre-legend-len-limit 22)

;; --- to be used maybe later ----

(def shaky false)


;; ------- hard-coded defaults -------

(def workday-start 480)

(def workday-end 1320)

;; -------------- scaling ---------------

(defonce mobile? js/window.roamAlphaAPI.platform.isMobile)

(defonce snail-scaler (if mobile? 0.7 1))

(def mob-width 450) ;; startovní hodnota pro šířku na mobilu

(def desk-width 600) ;; startovní hodnota pro šířku na desktopu


;; ---------- mostly visual dev settings ------------

(def reserve 10) ;; rezerva pro prostor mezi Nautilem a okraji

(def font-family "Inter, sans-serif")

(def font-size (if mobile? 12 15))

(def snail-blueprint-outer-radiuses
  "[95 100 105 110 115 120 125 130 145 140 140 130 125 120 115 110 105 100 95 90 85 80 75 70]
                                   8h  9h  10h 11h 12h 13h 14h 15h 16h 17h 18 19 20 21 22 23"
  [0  0   0   0   0   0   0   0   145 140 140 130 125 120 115 110 105 100 95 90 85 80 75 70])

(def snail-inner-radius (* 50 snail-scaler))

(defn outer-radius-at [t]
  (* (nth snail-blueprint-outer-radiuses t) snail-scaler))


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

;; --------- math is beautiful ---------

(def pi js/Math.PI)

(defn abs [x] (js/Math.abs x))

(defn cos [x] (js/Math.cos x))

(defn sin [x] (js/Math.sin x))

(defn round2 [num]  ;; zaokrouhluje na dvě desetinná místa
  (-> (* num 100)
      (js/Math.round)
      (/ 100.0)))

; (defn parse-int [v] (js/parseInt v))

(defn parse-int [v] (int v))

(defn angle->rad [angle]
  (* (- 180 angle) (/ pi 180)))

(defn pos-sweep-angle
  "Správně vypočítá úhlový rozsah"
  [start-radians end-radians]
  (- (* 2 pi) (if (> end-radians start-radians)
                (- end-radians start-radians)
                (+ (- end-radians start-radians) (* 2 pi)))))

(defn pos-sweep-angle-mid
  "Správně vypočítá střed úhlového rozsahu"
  [start-radians end-radians]
  (+ end-radians (/ (pos-sweep-angle start-radians end-radians) 2)))

(defn min->angle [minutes]
  (mod (/ (- minutes 540) 2) 360))

;; --------------- legend collision solution -----------

(defn between [x a b] ;; testuje, zda se x nachází mezi a a b
  (and (>= x a) (<= x b)))

(defn collide? [new-rect any-rect]
  (let [ntlx (:x new-rect)
        ntly (:y new-rect)
        nbrx (+ (:x new-rect) (:w new-rect))
        nbry (+ (:y new-rect) (:h new-rect))
        tlx (:x any-rect)
        tly (:y any-rect)
        brx (+ (:x any-rect) (:w any-rect))
        bry (+ (:y any-rect) (:h any-rect))]
    (not (or (< nbrx tlx)
             (> ntlx brx)
             (< nbry tly)
             (> ntly bry)))))

(defn collides? [new-rect rects] ;; testuje, zda se new-rect nepřekrývá s některým z rects
  (boolean (some #(collide? new-rect %) rects)))

(defn iterate-rect-place
  "vrací nové souřadnice new-rect, který se nepřekrývá s žádným z rects; 
   radians je úhel, pod kterým se new-rect snaží nejdřív umístit; 
   radius je vzdálenost od středu; 
   radians-span je maximální úhlová odchylka
   text je text legendy, který se vypisuje do konzole pro debug" ;; FIXME odstranit později
  [new-rect rects start-radians start-radius text center-x center-y]
  (loop [radians start-radians
         radius start-radius
         angle-offset  0
         radius-offset 0
         counter 0 ;; počet pokusů
         trying (if (or (between radians 1.05 2.09) (between radians -2.09 -1.05)) :radius :angle)]
    (let [max-radians-span (/ pi 15)
          min-radians (- radians (/ max-radians-span 2))
          max-radians (+ radians (/ max-radians-span 2))
          max-radius center-y
          x (+ center-x (* (cos radians) radius))
          y (+ center-y (* (sin radians) radius))
          horizontal-offset (if (or (> radians (/ pi 2)) (< radians (- (/ pi 2)))) (:w new-rect) 0)
          vertical-offset (/ (:h new-rect) 2)
          new-rect (assoc new-rect :x (- x horizontal-offset) :y (- y vertical-offset) :radians radians)
          colliding? (collides? new-rect rects)]
      (pprint?debug text " TRYING: " trying " x: " (round2 x) " y: " (round2 y) " radius: " (round2 radius) " radians: " (round2 radians)
                    " radius-offset " radius-offset
                    " angle-offset: " (round2 angle-offset) " colliding?: " colliding? " counter " counter)
      (if (or (> counter 25) (not colliding?))
        new-rect
        (if (= trying :radius) ;; zvětšujeme poloměr
          (if (< radius max-radius)
            (recur radians
                   (+ start-radius radius-offset)
                   angle-offset
                   (if (or (= 0 radius-offset) (pos? radius-offset))
                     (- (+ radius-offset 3))
                     (+ 3 (- radius-offset)))
                   (inc counter)
                   :radius)
            (recur radians start-radius angle-offset radius-offset 0 :angle))
          (if (and (> radians min-radians) (< radians max-radians))
            (recur (+ start-radians angle-offset)
                   radius
                   (if (or (= 0 angle-offset) (pos? angle-offset))
                     (- (+ 0.03 angle-offset))
                     (+ 0.03 (- angle-offset)))
                   radius-offset
                   (inc counter)
                   :angle)
            (recur start-radians start-radius angle-offset radius-offset 0 :radius)))))))

(defn get-legend-rect
  "vrací nový obdélník legendy, který se nepřekrývá s žádným z rects"
  [rects text radians radius center-x center-y]
  (let [w (* (/ font-size 1.5) (min (count text) legend-len-limit))  ;; šířka obdélníku legendy dle délky textu a cca šířky písma
        h (* 1.2 font-size)                          ;; výška obdélníku legendy dle cca výšky písma
        new-text-rect (if rects
                        (assoc
                         (iterate-rect-place {:w w :h h}               ;; velikost obdélníku legendy
                                             rects                     ;; úložiště obdélníků legendy 
                                             (- radians)               ;; směr pokusu o 1. umístění
                                             radius                    ;; vzdálenost od středu
                                             text
                                             center-x center-y)
                         :text text)                                   ;; FIXME jen pro debug              
                        {})]
    new-text-rect))

;; --------------- o`clock ----------------------

(defonce now-time-atom (r/atom 0))

(defonce minute-updater
  (js/setInterval
   #(let [now (new js/Date (.now js/Date))]
      (reset! now-time-atom (+ (* (.getHours now) 60)
                               (.getMinutes now))))
   10000))

;; -------------- roam db queries ----------

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

(defn get-block-str [block]
  (->> (rd/pull
        [:block/uid :block/string]
        [:block/uid block])
       :block/string))

(defn extract-ref [s]
  (let [[_ b u a] (re-find #"^(.*)\(\(([a-zA-Z0-9\-\_]{9})\)\)(.*$)" s)]
    (if u
      (str b (get-block-str u) a)
      s)))

;; ---------------- helpers ----------------------

(defn update-opacity-str [color opacity]
  (let [s (subs color 0 (- (count color) 5))
        new-color (str s "," opacity ")")]
    new-color))

(defn shake-if [shaky]
  (if shaky (- (rand-int 4) 2) 0))


;; --------------- text parsers --------------------

(defn time-str-to-minutes [time-str]
  (let [[h m] (str/split time-str #":")]
    (+ (if m (parse-int m) 0) (* 60 (parse-int h)))))

(defn parse-time-range [s]
  (let [range-format #"(?:\d{1,2}(?::\d{1,2})?)\s*(?:-|–|až|to)\s*(?:\d{1,2}(?::\d{1,2})?)"
        range-str (re-find range-format s)]
    (if range-str
      (let [[start-str end-str] (str/split range-str #"\s*(?:-|–|až|to)\s*")
            cleaned-str (str/replace s range-str "")]
        {:range [(time-str-to-minutes start-str) (time-str-to-minutes end-str)]
         :cleaned-str cleaned-str})
      {:range nil :cleaned-str s})))

(defn parse-duration [s]
  (let [duration-format #"(\d{1,3})(min|m)"
        duration-match (re-find duration-format s)]
    (if duration-match
      (let [duration-str (first duration-match)
            cleaned-str (str/replace s duration-str "")]
        {:duration (parse-int (second duration-match))
         :cleaned-str cleaned-str})
      {:duration nil :cleaned-str s})))

(defn parse-progress [s]
  (let [progress-format #"(\d{1,3})(\%)"
        progress-match (re-find progress-format s)]
    (if progress-match
      (let [progress-str (first progress-match)
            cleaned-str (str/replace s progress-str "")]
        {:progress (parse-int (second progress-match))
         :cleaned-str cleaned-str})
      {:progress nil :cleaned-str s})))

(defn parse-done-time [s]
  (let [done-time-format #"d(\d{1,2}(?::\d{1,2})?)"
        done-time-match (re-find done-time-format s)]
    (if done-time-match
      (let [[_ done-time-str] done-time-match
            [h m] (str/split done-time-str #":")
            cleaned-str (str/replace s (str "d" done-time-str) "")]
        {:done-at (+ (if m (parse-int m) 0) (* 60 (parse-int h)))
         :cleaned-str cleaned-str})
      {:done-at nil :cleaned-str s})))

(defn parse-DONE [s]
  (let [done-format #"\{\{\[\[DONE\]\]\}\}"
        done-found? (re-find done-format s)]
    (if done-found?
      {:done true
       :cleaned-str (str/replace s done-format "")}
      {:done false :cleaned-str s})))

(defn parse-rest [s]
  (-> s
      ;; Remove specific Roam markers (TODO, DONE, etc.)
      (str/replace #"\{\{\[\[TODO\]\]\}\}" "")
      (str/replace #"\{\{\[\[DONE\]\]\}\}" "")

      ;; Extract and format links
      (str/replace #"\[\[(.*?)\]\]" "$1")
      (str/replace #"\[(.*?)\]\((.*?)\)" "$1")

      ;; Remove other special formatting (bold, italic, etc.)
      (str/replace #"\*\*(.*?)\*\*" "$1")
      (str/replace #"\_\_(.*?)\_\_" "$1")
      (str/replace #"\^\^(.*?)\^\^" "$1")

      ;; Replace custom tags with their symbols or texts
      (str/replace #"\#hoří" "🔥")
      (str/replace #"\#čekám" "⏳")
      (str/replace #"\#hluboká" "📵")
      (str/replace #"\#cesta" "cesta➡︎")

      ;; Trim whitespace
      (str/trim)))

(defn parse-row-params [s settings]
  (let [;_ (println "STARTUJEME s s: " s)
        {:keys [range cleaned-str]} (parse-time-range s)
        ;_ (println "range: " range " cleaned-str: " cleaned-str)
        {:keys [duration cleaned-str]} (parse-duration cleaned-str)
        ;_ (println "duration: " duration " cleaned-str: " cleaned-str)
        {:keys [done-at cleaned-str]} (parse-done-time cleaned-str)
        ;_ (println "done-time: " done-at " cleaned-str: " cleaned-str)
        {:keys [done cleaned-str]} (parse-DONE cleaned-str)
        ;_ (println "done: " done " cleaned-str: " cleaned-str)
        {:keys [progress cleaned-str]} (parse-progress cleaned-str)
        ;_ (println "progress: " progress " cleaned-str: " cleaned-str)
        description (parse-rest cleaned-str)
        ;_ (println "description: " description)
        event-type (if range :meeting :todo)]
    (-> {:description description
         :duration (or duration (:default-duration settings))
         :start (if done-at (abs (- done-at duration)) (first range))
         :end (or done-at (second range))
         :done done
         :done-at (if done done-at nil)
         :progress (or progress 0)}
        (assoc event-type true))))

;; --------------- fill day ----------------------

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
                ;; pokud další meeting začíná později než aktuální čas
                (if (and next-todo (< (+ time (:duration next-todo)) (:start next-meeting)) (<= (:start-after next-todo) time))
                    ;; pokud je další todo a končí dřív než další meeting začíná, 
                    ;; a současně začíná po start-after (tedy konci předchozího meetingu, po němž bylo umístěno v seznamu)
                  (recur (rest todos) meetings (+ time (:duration next-todo)) (conj result (assoc next-todo :start time :end (+ time (:duration next-todo)))))
                  (recur todos meetings (:start next-meeting) (conj result {:freetime true :start time :end (:start next-meeting)})))
                (recur todos (rest meetings) (:end next-meeting) (conj result next-meeting)))
              (if next-todo
                (recur (rest todos) [] (+ time (:duration next-todo)) (conj result (assoc next-todo :start time :end (+ time (:duration next-todo)))))
                (recur [] [] time result)))))))))

;; --------------- reading Roam database ----------------------

(defn eval-state [*get-children]
  (:block/children @*get-children))

(defn get-children-strings [block-uid]
  (r/with-let [*get-children-atom (rdr/pull
                                   [{:block/children [:block/string :block/order]}]
                                   [:block/uid block-uid])
               *children (r/track eval-state *get-children-atom)]
    (map (comp extract-ref :block/string)
         (->> @*children
              (sort-by :block/order)))))

;; --------------- slice component ----------------------

(defn bent-line-component
  [legend-start-x legend-start-y text-x text-y color]  ;; TODO upravit výpočet pro zakončení na nejbližší straně rectu (vyžaduje úhel)
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
  [angle radius center-x center-y]
  (let [radians (angle->rad angle)]
    [(+ center-x (* (cos radians) radius))
     (- center-y (* (sin radians) radius))]))

(defn create-arc-path
  "Constructs the SVG path for the arc based on start and end angles and radii."
  [start-angle end-angle inner-radius outer-radius center-x center-y]
  (let [start-radians (angle->rad start-angle)
        end-radians (angle->rad end-angle)
        start-coord-outer (calculate-coordinates start-angle outer-radius center-x center-y)
        end-coord-outer (calculate-coordinates end-angle outer-radius center-x center-y)
        start-coord-inner (calculate-coordinates start-angle inner-radius center-x center-y)
        end-coord-inner (calculate-coordinates end-angle inner-radius center-x center-y)
        large-arc-flag (if (>= (pos-sweep-angle start-radians end-radians) pi) 1 0) #_(if (>= (abs (- end-angle start-angle)) 180) 1 0)]
    (str "M" (first start-coord-outer) "," (second start-coord-outer)
         " A" outer-radius "," outer-radius " 0 " large-arc-flag " 1 " (first end-coord-outer) "," (second end-coord-outer)
         " L" (first end-coord-inner) "," (second end-coord-inner)
         " A" inner-radius "," inner-radius " 0 " large-arc-flag " 0 " (first start-coord-inner) "," (second start-coord-inner)
         "Z")))

(defn slice
  "Vykreslí a vybarví výseč podle zadaných parametrů"
  [[start-angle end-angle inner-radius outer-radius center-x center-y]
   & {:keys [bg-color
             border-color
             legend-rect
             text
             timestamp
             stroke-dasharray ;; typ obrysu
             font-weight
             shaky      ;; "unsure hand style" allowed
             done?
             progress]}]  ;; has the todo been done?
  (let [hovered (r/atom false)
        start-radians (angle->rad (+ start-angle (shake-if shaky)))
        end-radians (angle->rad (+ end-angle (shake-if shaky)))
        mid-radians (pos-sweep-angle-mid start-radians end-radians)
        inner-radius (+ inner-radius (shake-if shaky))
        outer-radius (+ outer-radius (shake-if shaky))
        legend-line-start-x (+ (* (cos mid-radians) (+ 4 outer-radius)) center-x)
        legend-line-start-y (- center-y (* (sin mid-radians) (+ outer-radius 4)))
        legend-x (:x legend-rect)
        legend-y (:y legend-rect)
        legend-w (:w legend-rect)
        legend-h (:h legend-rect)
        legend-radians (- (:radians legend-rect))
        [legend-line-end-x legend-line-end-y] (cond (and (< legend-radians pi) (> legend-radians (/ pi 2))) [(+ legend-x (* 5/6 legend-w)) (+ legend-y legend-h)] ; levý horní
                                                    (and (< legend-radians (/ pi 2)) (> legend-radians 0)) [legend-x (+ legend-y legend-h)] ; pravý horní
                                                    (and (< legend-radians 0) (> legend-radians (- (/ pi 2)))) [legend-x legend-y] ; pravý dolní
                                                    :else [(+ legend-x (* 5/6 legend-w)) legend-y]) ; levý dolní
        time-text-x (+ center-x (* (cos start-radians) (- outer-radius 10)))
        time-text-y (- center-y (* (sin start-radians) (- outer-radius 10)))
        border-color (if (= border-color nil) "none" border-color)
        stroke-dasharray (if (= stroke-dasharray nil) "2,2" stroke-dasharray)
        bg-color (if (= bg-color nil) "rgba(255,255,255,0)" bg-color)
        legend-color (if-not done? (update-opacity-str bg-color "1") (update-opacity-str bg-color "0.2"))
        font-weight (if font-weight font-weight "normal")
        path (create-arc-path start-angle end-angle inner-radius outer-radius center-x center-y)
        debug? @debug-state-atom
        dbg-radians-txt (if debug? (str (round2 start-radians) "–>" (round2 end-radians)) "")
        progress-str (if (and debug? progress) (str progress " % ") "")]
    [:g
     (when @debug-state-atom  [:circle {:cx center-x :cy center-y :r 4 :fill "red"}])
     ;; ⤵ tohle je hlavní componenta – výseč

     [:path
      {:d path
       :stroke-dasharray stroke-dasharray
       :fill bg-color
       :on-click #(js/console.log "Dílek " text " odkliknut!")
       :on-mouse-enter (fn [_] (reset! hovered true))
       :on-mouse-leave (fn [_] (reset! hovered false))
       :stroke border-color}
      (when @hovered [:g [:text {:x 20 :y 20} text]])]

     ;; ⤵ přidá legendu události
     (when (and text (not done?))
       [:g
        [bent-line-component legend-line-start-x legend-line-start-y legend-line-end-x legend-line-end-y legend-color]
        [:text {:x legend-x :y (+ legend-y legend-h)
                :text-anchor "start"
                :alignment-baseline "baseline"
                :font-weight font-weight
                :fill (if-not done? (update-opacity-str bg-color "1") (update-opacity-str bg-color "0.2"))}
         (if debug? (str dbg-radians-txt)
             (str progress-str (subs text 0 legend-len-limit)))]])
     (when (seq timestamp)
       ;; ⤵ přidá označení hodiny pro šablonu snaila
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


(defn snail-blueprint-component [color inner-radius center-x center-y]
  [:g (mapcat (fn [[start end timestamp]]
                [[slice [start end inner-radius (outer-radius-at timestamp) center-x center-y] :border-color color :timestamp (str timestamp)]])
              (concat (map vector (range 0 390 30) (range 30 390 30) (range 9 21))
                      [(vector 0 30 21) (vector 330 360 8)]))])

(defn central-label-component [[weekday date] center-x center-y]
  [:g
   [:text {:x center-x :y (- center-y 10) :alignment-baseline "middle" :text-anchor "middle" :font-weight "bold" font-size (str (- font-size 4))} weekday] ;; FIXME opravit font-size 
   [:text {:x center-x :y (+ center-y 10) :alignment-baseline "middle" :text-anchor "middle" :font-weight "bold" font-size (str (- font-size 4))} date]
   (when @debug-state-atom [:text {:x center-x :y (+ center-y 30) :alignment-baseline "middle" :text-anchor "middle" :font-size font-size} @now-time-atom])])


(defn calculate-slice-params [event index daily-page?]
  (let [outer-radius (outer-radius-at (mod (quot (int (:start event)) 60) (count snail-blueprint-outer-radiuses)))
        start-angle (min->angle (:start event))
        end-angle (min->angle (:end event))
        todo? (:todo event)
        done-at (:done-at event)
        done? (if (:done event) true false)
        meeting? (:meeting event)
        expired? (and meeting? (>= @now-time-atom (:end event)))
        todo-bg-color (nth todo-color-palette (mod index (count todo-color-palette)))
        meeting-color (nth meeting-color-palette (mod index (count meeting-color-palette)))]
    #_(pprint-all "calc-slice-params:::: start " (:start event) " end-angle: " (:end event) " outer-radius: " outer-radius)
    {:start-angle start-angle
     :end-angle end-angle
     :bg-color (cond
                 meeting? (if (and daily-page? expired?) "rgba(128,128,128,0.1)" meeting-color)
                 todo? (if done-at "rgba(128,128,128,0.1)" todo-bg-color)
                 :else nil)
     :done done?
     :outer-radius outer-radius}))

;; (when @show-done-atom? ;; má-li ukazovat hotové úkoly, tak je ukáže v šedé
;;        (map (fn [event] [event-slice-component event 1 nil snail-inner-radius nil center-x center-y]) done-todos))

(defn event-slice-component [event index legend-rect inner-radius daily-page? center-x center-y]
  (let [{:keys [start-angle end-angle bg-color done outer-radius]} (calculate-slice-params event index daily-page?)
        description (:description event)
        progress (:progress event)]
    [slice
     [start-angle end-angle inner-radius outer-radius center-x center-y]
     :bg-color bg-color
     :text description
     :shaky shaky
     :done? done
     :font-weight "bold"
     :legend-rect legend-rect
     :progress progress]))

(defn events->slices
  "vrací svg vektor všech slice komponentů + seznam obdélníků legendy"
  [events daily-page-atom? center-x center-y]
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
            new-rect (get-legend-rect rects text mid-radians (+ (if mobile? 0 30) radius) center-x center-y)]
        (println?debug "RADIUS: " radius)
        (recur (inc i) (rest events) (conj rects new-rect) (conj all-slice-components (event-slice-component event i new-rect snail-inner-radius @daily-page-atom? center-x center-y))))
      [all-slice-components rects])))

(defn events->new-dimensions
  "vrací nový střed a šířku, aby se události daly zarovnat"
  [events center-x center-y]
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
            new-rect (get-legend-rect rects text mid-radians (+ (if mobile? 0 30) radius) center-x center-y)]
        (println?debug "RADIUS: " radius)
        (pprint?debug new-rect)
        (println?debug "LEFT-MIN: " left-min " RIGHT-MAX: " right-max " WIDTH: " (- right-max left-min))
        (recur (inc i) (rest events) (conj rects new-rect) (min left-min (:x new-rect)) (max right-max (+ (:x new-rect) (:w new-rect))) (min top-min (:y new-rect)) (max bottom-max (+ (:y new-rect) (:h new-rect)))))
      [(+ reserve (- center-x left-min))
       (+ reserve (- right-max left-min))
       (+ reserve (- center-y top-min))
       (+ (* 3 reserve) (- bottom-max top-min))])))

(defn show-events [events-state daily-page-atom? show-done-atom? page-title dimensions]
  (let [[events done-todos] @events-state
        old-width (js/Math.round (:width dimensions))
        old-height (js/Math.round (:height dimensions))
        [center-x suggested-width center-y suggested-height] (events->new-dimensions events (/ old-width 2) (/ old-height 2))
        [all-slice-components rects] (events->slices events daily-page-atom? center-x center-y)]   ;; rects jsou tam jen kvůli debugu, jinak vrací svg vektor
    [:svg {:width (str suggested-width) :height (str suggested-height)
           :xmlns "http://www.w3.org/2000/svg"
           :font-family font-family
           :font-size font-size}
     [:g
      [snail-blueprint-component snail-template-color snail-inner-radius center-x center-y]
      (when  @show-done-atom? ;; má-li ukazovat hotové úkoly, tak je ukáže v šedé
        (map (fn [event] [event-slice-component event 1 nil snail-inner-radius nil center-x center-y]) done-todos))
      all-slice-components ;; zobrazení všech událostí
      (when @daily-page-atom?   ;; ukáže aktuální čas pomocí úzké výseče
        [slice [(- (min->angle @now-time-atom) 1) (+ (min->angle @now-time-atom) 1) 0 center-y center-x center-y] :bg-color clock-hand-color])
      [central-label-component (str/split page-title #"," 2) center-x center-y]
      (when @debug-state-atom ;; čistě pro účely debugu ⤵
        [:g
         [draw-debug-rects rects]
         [:text {:x "0" :y "450" :text-anchor "start"}
          "Suggested w: " suggested-width
          ;" Width: " (:width @dimensions-atom)
          ;" Height: " (:height @dimensions-atom)
          " Center-x: " center-x
          " Center-y: " (js/Math.round center-y)]
         [:circle {:cx center-x :cy center-y :r 200 :fill "none" :stroke "black" :stroke-width 1}]])]]))

(defn add-start-after
  "přidává událostem čas, kdy končí, aby úkoly umístěné po meetingu věděly, že nesmí začít dřív"
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

(defn populate-events
  "vrací vektor [eventy, hotové_s_done-at]"
  [block-uid plan-from-time settings]
  (let [text->events (-> (mapv parse-row-params (get-children-strings block-uid), settings)
                         (add-start-after))
        filled-day [(-> text->events
                        (fill-day workday-start plan-from-time)) (filter #(:done-at %) text->events)]]
    filled-day)) ;; structure of filled-day: [[events] [done-todos]]
;; structure of events: [{:description "text" :duration 30 :start 0 :end 30 :done? false :done-at nil :start-after 0 :todo true :meeting false :freetime false :progress 0}] }]

(defn reset-now-time-atom [now-time-atom]
  (reset! now-time-atom
          (let [now (new js/Date (.now js/Date))
                minutes (+ (* (.getHours now) 60) (.getMinutes now))]
            minutes)))

(defn switch-done-visibility-button [show-done-state]
  [:button
   {:on-click #(swap! show-done-state not)
    :style {:background-color "#5B5B5BBF", :color "rgb(254,254,254)"
            :margin "8px"
            :border-radius "8px"
            :display "inline-block"}}
   (if @show-done-state
     (str "✅ showing done")
     (str "🙈 hiding done"))])

(defn switch-debug-button []
  [:button
   {:on-click #(swap! debug-state-atom not)
    :style {:background-color "#5B5B5BBF", :color "rgb(254,254,254)"
            :margin "8px"
            :border-radius "8px"
            :display "inline-block"}}
   (if @debug-state-atom
     (str "debug is on")
     (str "🪲 debug is off"))])

(defn main [{:keys [block-uid]} & args]
  (reset-now-time-atom now-time-atom)
  (let [dimensions {:width (if mobile? mob-width desk-width)
                    :height (* 0.7 (if mobile? mob-width desk-width))}
        show-debug-button? (= :debug (last args))
        arg1 (first args)
        default-duration (if (and arg1 (>= (int arg1) 5) (<= (int arg1) 30)) (int (first args)) pre-default-duration)
        settings {:default-duration default-duration
                  :show-debug-button? show-debug-button?}
        show-done-state (r/atom true)
        daily-page-atom? (r/atom (daily-page? block-uid))
        page-title (page-title block-uid)
        plan-from-time (if @daily-page-atom? @now-time-atom workday-start)
        events-state (r/atom (populate-events block-uid plan-from-time settings))]
    (println?debug "Dimensions-atom: " dimensions)
    (println?debug "Argumenty roam/renderu: " args)
    [:div
     (if-not (nil? @events-state)
       [show-events events-state daily-page-atom? show-done-state page-title dimensions settings]
       (reset! events-state (populate-events block-uid plan-from-time settings)))
     [:div
      [switch-done-visibility-button show-done-state]
      (when show-debug-button? [switch-debug-button])]]))