(ns clobber-test)

(defn main
  []
  (.log js/console "original main"))


(js/setTimeout #(main) 1000)
