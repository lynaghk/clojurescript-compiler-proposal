(ns com.keminglabs.cljs-proposal.middleware
  (:require [cljs.analyzer :as ana]
            [cljs.compiler :as compiler]
            [clojure.set :refer [subset?]])
  (:import java.io.StringReader))


(defn with-forms
  "Adds :forms key to the compilation map, corresponding to forms read from :cljs-src string."
  [compilation-map]
  ;;TODO document ex-info map shape
  (let [{:keys [cljs-src] :as m} compilation-map]
    (assoc m :forms (ana/forms-seq (StringReader. cljs-src)))))



(defn expression-map?
  "From defacto specification in docstring of cljs.analyzer/analyze."
  [x]
  (subset? #{:form :op :env} (set (keys x))))

(def namespace-map-keys
  #{:name :doc :requires :requires-macros :uses :uses-macros :imports :excludes})
(defn namespace-map?
  [x]
  (subset? namespace-map-keys (set (keys x))))

(def cljs-user-namespace-map
  {:name 'cljs.user
   :doc nil
   :requires nil
   :requires-macros nil
   :uses nil
   :uses-macros nil
   :imports nil
   :excludes #{}})

(defn with-analysis
  "Adds :expressions and :namespace keys to the compilation map, from analysis of :forms.
   All forms should be part of the same namespace."
  [compilation-map]
  (let [{:keys [forms] :as m} compilation-map
        expressions (binding [ana/*cljs-ns* 'cljs.user]
                      (doall (for [form forms]
                               (ana/analyze (ana/empty-env) form))))
        namespace (-> (or (first (filter namespace-map? expressions))
                          cljs-user-namespace-map)
                      (select-keys namespace-map-keys))]

    (assoc m :expressions expressions :namespace namespace)))

(defn with-js
  "Adds :js key to the compilation map, from emission of :expressions."
  [compilation-map]
  (let [{:keys [expressions] :as m} compilation-map]
    (assoc m :js (with-out-str
                   (doseq [e expressions]
                     (compiler/emit e))))))

