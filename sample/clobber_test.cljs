(ns clobber-test
  (:require [b :refer [two] :as b]))

(defn main
  []
  (.log js/console "original main")
  (.log js/console two)
  (.log js/console b/two))


(js/setTimeout #(main) 1000)
