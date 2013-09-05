(ns com.keminglabs.cljs-proposal.t-middleware
  (:require [com.keminglabs.cljs-proposal.middleware :refer :all]
            [clojure.test :refer [deftest is]]))

(def cljs-sample
  "(foo 1 2) (bar 3 4)")

;;with-forms should add seq of forms to compilation map
(is (= (with-forms {:cljs-src cljs-sample})
       {:cljs-src cljs-sample :forms '((foo 1 2) (bar 3 4))}))


;;with-analysis

(let [{:keys [expressions namespace] :as m} (with-analysis {:cljs-src cljs-sample :forms '((foo 1 2) (bar 3 4))})]
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
        :requires-macros nil
        :uses nil
        :uses-macros nil
        :imports nil
        :excludes #{}}))