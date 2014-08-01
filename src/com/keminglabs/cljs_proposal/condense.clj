(ns com.keminglabs.cljs-proposal.condense
  (:require [com.keminglabs.cljs-proposal.middleware :as m]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [cljs.closure :as closure]
            [cljs.js-deps :as deps])
  (:import (com.google.javascript.jscomp DependencyOptions SourceFile CheckLevel)))

(defn compile-cljs
  "Fully realized compilation map for provided ClojureScript string."
  [cljs-src]
  (-> {:cljs-src cljs-src}
      m/with-forms
      m/with-analysis
      m/with-js))

(defn optimize
  "Condenses given compilation maps into a single JavaScript string using the Google Closure compiler."
  [compilation-maps opts]

  (when (empty? compilation-maps)
    (throw (Error. "No compilation maps provided to `optimize`")))

  (let [closure-compiler (closure/make-closure-compiler)

        compiler-options (doto (closure/make-options opts)

                           ;;TODO: the checkprovides and checkrequires should only be enabled for whitespace and simple modes.
                           ;;As it turns out, Closure will cry wolf in advanced mode---it seems to get confused by its own name munging, lifting, &c.
                           ;;(.setClosurePass false)
                           ;; (.setCheckProvides CheckLevel/WARNING)
                           ;; (.setCheckRequires CheckLevel/WARNING)
                           (.setDependencyOptions (doto (DependencyOptions.)
                                                    (.setDependencyPruning false)
                                                    ;;(.setEntryPoints ["a"])
                                                    (.setDependencySorting true))))
        externs          []
        js-source-files  (concat [(SourceFile/fromCode "goog/base.js" (slurp (deps/goog-resource "goog/base.js")))]
                                 (for [{:keys [js provides]} compilation-maps]
                                   (SourceFile/fromCode (str (first provides)) js)))]

    (let [res (-> (doto closure-compiler
                    (.compile externs js-source-files compiler-options)
                    (.check))
                  (.getResult))]

      ;;TODO: convert errors to ClojureScript data so they can be easily consumed by downstream toolin'
      ;;(AKA the wonderful world of regex...)
      (doseq [e (.errors res)]
        (println (str (.-sourceName e) ": " (.-description e))))
      (doseq [e (.warnings res)]
        (prn (.-description e)))

      (when (.success res) ;;note: compilation is always successful unless .check is called.
        (.toSource closure-compiler)))))


(defn missing-namespaces
  "Returns the set of namespaces required but not provided by `compilation-maps`."
  [compilation-maps]
  (let [provides (set (mapcat :provides compilation-maps))
        requires (set (mapcat :requires compilation-maps))
        missing (set/difference requires provides)]

    (when-not (empty? missing)
      missing)))


(def goog-closure-namespaces
  "Map of namespace -> {:provides #{} :requires #{} :js ''}"
  (reduce (fn [m goog-namespace]
            (let [provides (set (map symbol (:provides goog-namespace)))
                  requires (set (map symbol (:requires goog-namespace)))]
              (merge m (zipmap provides
                               (repeat {:provides provides
                                        :requires requires
                                        :js (slurp (io/resource (:file goog-namespace)))})))))
          {} (deps/goog-dependencies)))


(defn resolve-deps
  "Collects all compilation maps for `namespaces` and their dependencies from `compilation-maps-by-namespace`."
  [namespaces compilation-maps-by-namespace]
  (loop [namespaces namespaces
         visited #{}
         res #{}]
    (if (empty? namespaces)
      res
      (let [[n & ns] (seq namespaces)]
        (if-let [m (get compilation-maps-by-namespace n)]
          (if (contains? visited n)
            (recur ns visited res)
            (do
              (recur (set (concat ns (:requires m)))
                     (conj visited n)
                     (conj res m))))
          (recur ns (conj visited n) res))))))

(defn index-by
  "Like group-by, but when you know your keys are unique"
  [f coll]
  (reduce (fn [m v] (assoc m (f v) v)) {} coll))

(comment
  (require '[clojure.pprint :refer [pprint]])

  (def compile-cljs*
    (memoize compile-cljs))

  (->> [(io/resource "cljs/core.cljs")
        ;;"sample/a.cljs" "sample/b.cljs"
        "sample/macro_test.cljs"
        ]
       (map slurp)
       ;;TODO: disable ClojureScript's built in warnings, which are printed during analysis.
       ;;Linting and warning emission should be separate functions that are invoked with sets of compilation maps.
       (map compile-cljs*)
       (index-by (comp first :provides))
       (merge goog-closure-namespaces)
       (resolve-deps #{'macro-test})
       (optimize)
       (spit "foo.js"))

  )
