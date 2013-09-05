(ns com.keminglabs.cljs-proposal.middleware
  (:require [cljs.analyzer :as ana])
  (:import java.io.StringReader))


(defn with-forms
  "Reads forms from :cljs-src key of provided `compilation-map` and adds result to :forms key."
  [compilation-map]
  ;;TODO document ex-info map shape
  (let [{:keys [cljs-src] :as m} compilation-map]
    (assoc m :forms (ana/forms-seq (StringReader. cljs-src)))))