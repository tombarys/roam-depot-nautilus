(ns component-paste-ical
  (:require
   [clojure.string :as str]
   [roam.datascript :as rd]
   [roam.ui.ms-context-menu :as ms]
   [roam.block :as block]
   [promesa.core :as p]))

;; –––––– settings ––––––

(def children-blocks? true) ;; true = it adds descriptions to events (if they exist) as children
                            ;; false = just creates the event with time and title

(def highlight "^^")        ;; "^^" – highlights event rows
                            ;; "" – it does not

;; ------ end settings –––––

(defn to-24h [time-str]
  (let [[hours mins] (str/split time-str ":")
        pm? (re-find #"(?:pm|PM)" mins)]
    (str (if pm? (str (+ 12 (js/parseInt hours))) hours) 
         ":" 
         (str/replace mins #"(?:\sam|\sAM|\spm|\sPM)" ""))))

(defn extract-range [s]
  (let [range-format #"(?:\d{1,2}(?::\d{1,2})?(?:\s*(?:\sAM|\sPM|\sam|\spm))?)\s*(?:až|to)\s*(?:\d{1,2}(?::\d{1,2})?(?:\s*(?:\sAM|\sPM|\sam|\spm))?)"
        to-form #"(.+)\s(?:až|to)\s(.+)"
        full-range-str (re-find range-format s)
        [_ from-str to-str] (re-find to-form full-range-str)
        from-str-24 (to-24h from-str)
        to-str-24 (to-24h to-str)]
    (str from-str-24 "-" to-str-24)))

(defn update-block [block-uid text]
  (block/update {:block {:uid block-uid :string text}}))

(defn is-title? [s]
  (and (str/starts-with? s "**")
       (str/ends-with? s "**")))

(defn get-block-info [block]
  (let [{:keys [block/order block/string block/uid]} 
        (rd/pull [:block/uid :block/string :block/order]
                 [:block/uid block])]
    [order string uid]))

(defn create-children [parent-id text-vec]
  (doseq [text text-vec]
     (p/do! (-> (roam.block/create
            {:location {:parent-uid parent-id
                        :order :last}
             :block {:string text}})
         (.then #(js/console.log "create ok"))
         (.catch #(js/console.log "create fail" %))))))

(defn erase-block [block-uid]
  (p/do! 
    (-> (block/delete
       {:block {:uid block-uid}})
      (.then #(js/console.log "erase ok"))
      (.catch #(js/console.log "erase fail" %)))))

(defn extract-sorted-blocks [blocks] ;; sorts blocks by the order and returns vector
  (->> blocks
       (mapv get-block-info)
       (sort-by first)
       (into [])))

(defn go-through-blocks [blocks]
  (loop [blocks blocks
         event-title nil
         title-uid ""
         event-children []]
    (let [[_ block-text block-uid] (first blocks)]
      (if block-text                     
        (if (is-title? block-text)       
          (if event-title             
            (do
              (update-block title-uid (str highlight (extract-range (first event-children)) " " event-title highlight))
              (when children-blocks? (create-children title-uid (rest event-children)))
              (recur (rest blocks) block-text block-uid []))
            (recur (rest blocks) block-text block-uid event-children))
          (do
            (erase-block block-uid)
            (recur (rest blocks) event-title title-uid (conj event-children block-text))))
        (do
          (update-block title-uid (str highlight (extract-range (first event-children)) " " event-title highlight))
          (when children-blocks? (create-children title-uid (rest event-children))))))))

(defn main []
  (ms/add-command
   {:label "Parse iCal paste"
    :callback (fn [x]
                 (let [block-ids (mapv :block-uid (get (js->clj x :keywordize-keys true) :blocks))
                         sorted-infos (extract-sorted-blocks block-ids)]
                   (go-through-blocks sorted-infos)))}))

(main)
