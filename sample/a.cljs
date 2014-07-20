(ns a
  (:require [b :refer [two]]))

(defn add-two [x]
  (+ x two))

(.log js/console (add-two 1))

