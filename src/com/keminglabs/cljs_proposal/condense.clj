(ns com.keminglabs.cljs-proposal.condense
  (:require [com.keminglabs.cljs-proposal.middleware :as m]
            [cljs.closure :as closure])
  (:import (com.google.javascript.jscomp JSModule SourceFile)))

(defn with-js-module
  "Adds :js-module key to compilation map with instance of Google's JSModule, corresponding to :js source."
  [compilation-map]
  (let [{js :js
         {ns-symbol :name ns-requires :requires} :namespace} compilation-map
         ns-name (name ns-symbol)]

    (assert (not (empty? js)) "Compilation map doesn't contain JavaScript code under the :js key.")

    (assoc compilation-map
      :js-module (doto (JSModule. ns-name)
                   (.add (SourceFile/fromCode ns-name js))))))

(defn compile-cljs
  "Fully realized compilation map for provided ClojureScript string."
  [cljs-src]
  (-> {:cljs-src cljs-src}
      m/with-forms
      m/with-analysis
      m/with-js))

(defn add-dependencies-to-js-modules!
  "Wires up collection of compilation maps containing :js-module keys to depend on each other by bashing on JSModule objects.
Assumes each module provides a single namespace.
Returns nil.
See: http://javadoc.closure-compiler.googlecode.com/git/com/google/javascript/jscomp/JSModule.html
Throws ex-info if unmet or circular dependencies."
  [compilation-maps]
  ;;TODO: throw ex-info with helpful data about unmet or circular dependencies
  (let [modules-by-ns (into {} (for [m compilation-maps]
                                 [(get-in m [:namespace :name]) (:js-module m)]))]
    (doseq [m compilation-maps
            dep (-> m :namespace :requires vals)]
      (.addDependency (:js-module m) (modules-by-ns dep)))))

(defn optimize
  "Condenses given compilation maps into a single JavaScript string using the Google Closure compiler."
  [compilation-maps]
  ;;TODO: accept options
  (let [closure-compiler (closure/make-closure-compiler)

        compiler-options (closure/make-options {:optimizations :whitespace})

        externs []
        js-modules (-> (map (comp :js-module with-js-module) compilation-maps)
                       (doto add-dependencies-to-js-modules!)
                       JSModule/sortJsModules
                       seq)]

    (let [res (.compileModules closure-compiler externs js-modules compiler-options)]
      (when (.success res)
        (.toSource closure-compiler)
        ;;TODO: throw errors
        ))))






(comment
  (require '[clojure.pprint :refer [pprint]]
           '[clojure.java.io :as io])


  (->> ["sample/a.cljs" "sample/b.cljs"]
       (map slurp)
       (map compile-cljs)
       optimize)

  )