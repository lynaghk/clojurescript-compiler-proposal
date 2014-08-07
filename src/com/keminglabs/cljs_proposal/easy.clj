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

(def cljs-resources*
  (memoize util/cljs-resources))

(defn compile-ns
  "Compiles namespace and all its dependencies, returning a set of compilation maps"
  ([ns]
     (compile-ns ns nil))
  ([ns extra-paths]
     (let [compilation-maps (->> (cljs-resources*)
                                 (concat (util/cljs-files extra-paths))
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
  (time
    (do
      (doall (compile-ns 'macro-test ["sample"]))
      nil))

  (time
    (compile! 'macro-test
              {:source-paths ["sample"]
               :output-to "foo.js"
               :compiler {:optimizations :whitespace
                          :closure-warnings {:global-this :off}}}))


  ;;Even in whitespace-only mode, Closure compiler takes about 900 ms.
  ;;This is too long a delay for livereloadin', so lets try the following scheme:
  ;;
  ;; + first compile is Closure whitespace (for dependency sortin')
  ;; + later compiles are for the changed namespace only, which should clobber everythin' in the previous namespace

  ;;Try deliberate clobbering of namespaces

  (def the-ns 'clobber-test)

  (->> (c/optimize (compile-ns the-ns ["sample"])
                   {:optimizations :whitespace})
       (spit "foo.js"))

  ;;Running `foo.js` should print "original main"

  (def new-cljs
    "

 (ns clobber-test)

 (defn main
   []
   (.log js/console \"clobbered main\"))

")

  (let [new-js (-> new-cljs
                   com.keminglabs.cljs-proposal.easy/compile-cljs*
                   :js)]
    (spit "foo.js" new-js :append true))

  ;;Running `foo.js` now prints "clobbered main"
  )
