(ns com.keminglabs.cljs-proposal.condense
  (:require [com.keminglabs.cljs-proposal.middleware :as m]
            [cljs.closure :as closure]
            [cljs.js-deps :as deps])
  (:import (com.google.javascript.jscomp DependencyOptions SourceFile)))

(defn compile-cljs
  "Fully realized compilation map for provided ClojureScript string."
  [cljs-src]
  (-> {:cljs-src cljs-src}
      m/with-forms
      m/with-analysis
      m/with-js))

(defn optimize
  "Condenses given compilation maps into a single JavaScript string using the Google Closure compiler."
  [compilation-maps]
  ;;TODO: accept options
  (let [closure-compiler (closure/make-closure-compiler)

        compiler-options (doto (closure/make-options {:optimizations :whitespace})
                           (.setClosurePass true)
                           (.setDependencyOptions (doto (DependencyOptions.)
                                                    (.setDependencySorting true))))
        externs []
        js-source-files (for [{:keys [js namespace]} compilation-maps]
                          (SourceFile/fromCode (str (:name namespace)) js))]

    (let [res (.compile closure-compiler externs js-source-files compiler-options)]
      (if (.success res)
        (.toSource closure-compiler)

        ;;TODO: convert errors to ClojureScript data so they can be easily consumed by downstream toolin'
        (doseq [e (.errors res)]
          (prn (.-description e)))))))




(comment
  (require '[clojure.pprint :refer [pprint]]
           '[clojure.java.io :as io])

  (def compile-cljs*
    (memoize compile-cljs))

  (->> [;; (io/resource "cljs/core.cljs")
        "sample/a.cljs" "sample/b.cljs"]
       (map slurp)
       ;;TODO: disable ClojureScript's built in warnings, which are printed during analysis.
       ;;Linting and warning emission should be separate functions that are invoked with sets of compilation maps.
       (map compile-cljs*)

       optimize
       println
       ;;(spit "foo.js")
       )

  )
