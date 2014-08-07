(ns com.keminglabs.cljs-proposal.closure
  (:require 
            [clojure.java.io :as io]
            [cljs.closure :as closure]
            [cljs.js-deps :as deps])
  (:import (com.google.javascript.jscomp DependencyOptions SourceFile CheckLevel)))


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


(defn optimize
  "Condenses given compilation maps into a single JavaScript string using the Google Closure compiler."
  [compilation-maps opts]

  (when (empty? compilation-maps)
    (throw (Error. "No compilation maps provided to `optimize`")))

  (let [closure-compiler (closure/make-closure-compiler)

        compiler-options (doto (closure/make-options opts)
                           (.setClosurePass true)
                           ;;TODO: the checkprovides and checkrequires should only be enabled for whitespace and simple modes.
                           ;;As it turns out, Closure will cry wolf in advanced mode---it seems to get confused by its own name munging, lifting, &c.
                           ;; (.setCheckProvides CheckLevel/WARNING)
                           ;; (.setCheckRequires CheckLevel/WARNING)
                           (.setDependencyOptions (doto (DependencyOptions.)
                                                    (.setDependencyPruning false)
                                                    ;;(.setEntryPoints ["a"])
                                                    (.setDependencySorting true))))
        externs          (closure/load-externs opts)
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
