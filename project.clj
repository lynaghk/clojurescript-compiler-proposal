(defproject com.keminglabs/clojurescript-compiler-proposal "0.0.1-SNAPSHOT"
  :description "Structure the ClojureScript compiler in terms of namespace-level middleware"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1859"
                  :exclusions [org.apache.ant/ant]]])