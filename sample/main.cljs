(ns main
  "A namespace with more than just defs"
  (:require a))

(.log js/console (= 22 (a/add-x 11)))