(ns component
  (:require [roam.datascript :as rd]
            [reagent.core :as r]
            [roam.datascript.reactive :as rdr]
            [clojure.string :as str]))

;; ---------- GENERAL SETUP FOR SCRIPT START ------------
;; change values below depending on your preference

(def max-len 30)              ;; default maximum length for each of children
(def ellipsis "…")            ;; added to the end of each cut children (use "" for none)
(def todo-symbol "▢")        ;; minified TODO symbol 
(def done-symbol "✅")        ;; minified DONE symbol 
(def delimiter "﹢")          ;; symbol between children in compressed text
(def font-color "gray")     ;; preview font color
(def background-color "")     ;; preview background color, try e.g. "#9E9E9E63" ("" for none)

;; ------------------- SETUP END -----------------------

;; ---------- ADDITIONAL PER-BLOCK CONFIG  -------------
;; you can use a special parameter in component to overwrite 
;; default length defined by max-len parameter:
;;
;; Example: 
;;              {{[[roam/render]]: ((-vAPwsKWK)) 100}}
;;
;; prolongs the length of each children up to 100 chars 


(defn expand-uid->str [uid]
  (get (rd/pull
        [:block/string]
        [:block/uid uid]) :block/string))

(defn clean-roam-tags [s]
  #_(if (re-matches #"\(\((.*)\)\)"))
  #_(str/replace s #"\(\((.*)\)\)" "$1") ;; výměna '((asdlkjalsk))' za obsah
  (-> s
      (str/replace #"\#" "")
      (str/replace #"\[(.*)\]\(.*\)" "$1")
      (str/replace #"\{\{\[\[TODO\]\]\}\}" todo-symbol)
      (str/replace #"\{\{\[\[DONE\]\]\}\}" done-symbol)
      (str/replace #"\{\{.*\}\}" "")
      (str/replace #"\[\[" "")
      (str/replace #"\]\]" "")))

(defn get-children [block]
  (->>
   (get (rd/pull
         [{:block/children [:block/string :block/order]}]
         [:block/uid block]) :block/children)
   (sort-by :block/order)))

(defn cut-and-add [s local-len]
  (let [l (max max-len local-len)
        e (if (> (count s) l) ellipsis "")]
    (str (subs s 0 l) e)))

(defn children-preview [local-len all-children]
  (str/join delimiter
            (map #(-> (get % :block/string)
                      clean-roam-tags
                      (cut-and-add local-len))
                 all-children)))

(defn eval-state [*get-state]
  (get @*get-state :block/open))

(defn expanded? [the-uid]
  (r/with-let [*get-state-atom (rdr/pull [:block/open] [:block/uid the-uid])
               *state? (r/track eval-state *get-state-atom)]
    @*state?))

(defn main [{:keys [block-uid]} & local-len]
  (when-not (expanded? block-uid)
    [:span {:style {:line-height 1 :background-color background-color}}
     [:font {:size "1" :color font-color}
      (->> (get-children block-uid)
           (children-preview (first local-len)))]]))