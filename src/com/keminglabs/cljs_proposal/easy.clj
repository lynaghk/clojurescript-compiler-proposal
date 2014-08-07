(ns com.keminglabs.cljs-proposal.easy
  "An interface to the ClojureScript compiler with all of the magical classpath-slurping, dependency resolution, caching, and output-to-filesystem emission."
  (:require [com.keminglabs.cljs-proposal.middleware :as m]
            [com.keminglabs.cljs-proposal.closure :as c]
            [com.keminglabs.cljs-proposal.util :as util]
            [cljs.env :as env]
            [clojure.java.io :as io]))


(def env-with-cljs-core
  "A ClojureScript compiler env that has been side-affected by analysis of cljs.core."
  (let [env (env/default-compiler-env)]
    (doall (-> {:cljs-src (slurp (io/resource "cljs/core.cljs"))}
               (m/with-forms)
               (m/with-analysis env)))
    @env))


(defn compile-cljs
  "Generate compilation map for provided ClojureScript string."
  [cljs-src]
  (-> {:cljs-src cljs-src}
      m/with-forms
      (m/with-analysis env-with-cljs-core)
      m/with-js))


(def compile-cljs*
  (memoize compile-cljs))


(defn compile-ns
  "Compiles namespace and all its dependencies, returning a set of compilation maps"
  ([ns]
     (compile-ns ns nil))
  ([ns extra-paths]
     (let [compilation-maps (->> (util/cljs-resources extra-paths)
                                 (map slurp)
                                 (map compile-cljs*)
                                 (util/index-by (comp first :provides))
                                 (merge c/goog-closure-namespaces)
                                 (util/resolve-deps #{ns}))]
       (if (empty? compilation-maps)
         (throw (Error. (str "Could not find and compile namespace: " ns)))
         (if-let [missing (util/missing-namespaces compilation-maps)]
           (throw (Error. (str "Missing namespaces: " missing)))
           compilation-maps)))))


(defn compile!
  [ns opts]
  (->> (c/optimize (compile-ns ns (:source-paths opts))
                   (:compiler opts))
       util/add-wrapper
       (spit (:output-to opts "out.js"))))


(comment

  (compile! 'macro-test
            {:source-paths ["sample"]
             :output-to "foo.js"
             :compiler {:optimizations  :advanced
                        :closure-warnings {:global-this :off}}})

  )
