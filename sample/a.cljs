(ns a
  (:require [b :refer [x]]))

(defn add-x [z]
  (+ z x))