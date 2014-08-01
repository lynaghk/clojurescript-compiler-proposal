(ns com.keminglabs.cljs-proposal.test-macros)

(defmacro p
  "Print and return native JavaScript argument."
  [x]
  `(let [res# ~x]
     (.log js/console res#)
     res#))

(defmacro pp
  "Pretty print and return argument (uses `prn-str` internally)."
  [x]
  `(let [res# ~x]
     (.log js/console (prn-str res#))
     res#))
