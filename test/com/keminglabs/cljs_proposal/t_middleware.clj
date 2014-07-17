(ns com.keminglabs.cljs-proposal.t-middleware
  (:require [com.keminglabs.cljs-proposal.middleware :refer :all]
            [clojure.test :refer [deftest is]]))

(def cljs-sample
  "(+ 1 2) (+ 3 4)")

;;with-forms should add seq of forms to compilation map
(is (= (with-forms {:cljs-src cljs-sample})
       {:cljs-src cljs-sample :forms '((+ 1 2) (+ 3 4))}))


;;with-analysis

(let [{:keys [expressions namespace] :as m} (with-analysis {:cljs-src cljs-sample :forms '((+ 1 2) (+ 3 4))})]
  (is (every? identity #{expressions namespace}))
  (is (every? expression-map? expressions))
  (is (namespace-map? namespace)))

;;with a complex namespace
(is (= (:namespace (with-analysis {:forms '((ns foo
                                              "Awesome namespace"
                                              (:require a.b)))}))
       {:name 'foo
        :doc "Awesome namespace"
        :requires '{a.b a.b}
        :require-macros nil
        :uses nil
        :use-macros nil
        :imports nil
        :excludes #{}}))





;;single-namespace integration tests
(defn cljs->js
  [cljs-str]
  (-> {:cljs-src cljs-str}
      with-forms
      with-analysis
      with-js
      :js))

(= (cljs->js "(+ 1 2)")
   "((1) + (2));\n")

(= (cljs->js "(ns foo) (def bar 1)")
   "goog.provide('foo');\ngoog.require('cljs.core');\nfoo.bar = (1);\n")

(= (cljs->js "(ns foo) (bar 1)")
   "goog.provide('foo');\ngoog.require('cljs.core');\nfoo.bar.call(null,(1));\n")
