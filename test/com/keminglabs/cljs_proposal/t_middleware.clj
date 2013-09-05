(ns com.keminglabs.cljs-proposal.t-middleware
  (:require [com.keminglabs.cljs-proposal.middleware :refer :all]
            [clojure.test :refer [deftest is]]))

(def cljs-sample
  "(foo 1 2) (bar 3 4)")

;;with-forms should add seq of forms to compilation map
(is (= (with-forms {:cljs-src cljs-sample})
       {:cljs-src cljs-sample :forms '((foo 1 2) (bar 3 4))}))