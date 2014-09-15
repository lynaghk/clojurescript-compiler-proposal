(ns com.keminglabs.cljs-proposal.middleware
  (:require [cljs.analyzer :as ana]
            [cljs.env :as env]
            [cljs.compiler :as compiler]
            [clojure.set :refer [subset?]])
  (:import java.io.StringReader))


(defn with-forms
  "Adds :forms key to the compilation map, corresponding to forms read from :cljs-src string."
  [compilation-map]
  ;;TODO document ex-info map shape
  (let [{:keys [cljs-src] :as m} compilation-map]
    (env/ensure
     (assoc m :forms (doall (ana/forms-seq (StringReader. cljs-src)))))))

(defn expression-map?
  "From defacto specification in docstring of cljs.analyzer/analyze."
  [x]
  (subset? #{:form :op :env} (set (keys x))))

(def namespace-map-keys
  #{:name :doc :requires :require-macros :uses :use-macros :imports :excludes})

(defn namespace-map?
  [x]
  (subset? namespace-map-keys (set (keys x))))

(def cljs-user-namespace-map
  {:name 'cljs.user
   :doc nil
   :requires nil
   :require-macros nil
   :uses nil
   :use-macros nil
   :imports nil
   :excludes #{}})

(defn with-analysis
  "Adds :expressions and :namespace keys to the compilation map, from analysis of :forms.
   All forms should be part of the same namespace.
   You probably want to use a `compiler-env` that has been side-affected by analysis of cljs.core."
  [compilation-map compiler-env]
  (let [{:keys [forms] :as m} compilation-map
        expressions (env/with-compiler-env compiler-env
                      (binding [ana/*cljs-ns* 'cljs.user
                                ana/*cljs-warnings* (assoc ana/*cljs-warnings*
                                                      :undeclared-ns-form false)]

                        (doall (for [form forms]
                                 (ana/analyze (assoc (ana/empty-env) :ns (ana/get-namespace ana/*cljs-ns*))
                                              form)))))

        namespace (-> (or (first (filter namespace-map? expressions))
                          cljs-user-namespace-map)
                      (select-keys namespace-map-keys))]

    (assoc m
      :expressions expressions
      :namespace namespace
      :provides #{(:name namespace)}
      :requires (let [requires (set (vals (:requires namespace)))]
                  (if (= 'cljs.core (:name namespace))
                    requires
                    (conj requires 'cljs.core))))))

(defn with-js
  "Adds :js key to the compilation map, from emission of :expressions."
  [compilation-map]
  (let [{:keys [expressions] :as m} compilation-map]
    (assoc m :js (with-out-str
                   (doseq [e expressions]
                     (compiler/emit e))))))
