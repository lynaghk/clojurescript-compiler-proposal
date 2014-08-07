(ns com.keminglabs.cljs-proposal.util
  (:require [clojure.set :as set]
            [clojure.java.io :as io]
            [clojure.java.classpath :as cp]))


(defn index-by
  "Map of (f x) -> x for all x in provided coll. Assumes (f x) is a bijection over all x in coll."
  [f coll]
  (reduce (fn [m v] (assoc m (f v) v)) {} coll))


(defn cljs-resources
  "All cljs resources in JARs and cljs files on classpath"
  []
  (->> (mapcat cp/filenames-in-jar (cp/classpath-jarfiles))
       (concat (map #(.getPath %) (mapcat file-seq (cp/classpath-directories))))
       (filter #(.endsWith % ".cljs"))
       set
       (map #(or (io/resource %) (io/file %)))))


(defn cljs-files
  [paths]
  (->> (map #(.getPath %) (mapcat #(file-seq (io/file %)) paths))
       (filter #(.endsWith % ".cljs"))
       set
       (map io/file)))


(defn missing-namespaces
  "Returns the set of namespaces required by (but not provided by) `compilation-maps`."
  [compilation-maps]
  (let [provides (set (mapcat :provides compilation-maps))
        requires (set (mapcat :requires compilation-maps))
        missing (set/difference requires provides)]

    (when-not (empty? missing)
      missing)))


(defn resolve-deps
  "Collect all compilation maps for `namespaces` and their dependencies from `compilation-maps-by-namespace`."
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


(defn add-wrapper
  [js]
  (str ";(function(){\n" js "\n})();\n"))
