;;Structure the ClojureScript compiler in terms of namespace-level middleware.

(defn compile
  [compilation-map]
  ;;(Maps shown after each function below indicate intermediate result---without comments for syntax-highlighting clarity only.)
  (-> compilation-map
      {:cljs cljs-string ;;only required key
       ;;but this might be nice to have...
       :file java.io.file-object}

      with-forms ;;can throw ex-info with reader error data
      {:cljs "a clojurescript string"
       :forms '(seq-of-cljs-forms)}

      with-analysis ;;can throw ex-info with analysis errors (unresolved var, and so on)
      {:cljs "a clojurescript string"
       :forms '(seq-of-cljs-forms)
       :expressions '(seq-of-cljs-expression-objects)
       :namespace {:name com.keminglabs.example.core
                   :doc nil
                   :requires {other com.keminglabs.example.other} :requires-macros nil
                   ;;this existing format doesn't allow for renames; while we're here, should we change it up?
                   :uses {foo com.keminglabs.example.other} :uses-macros nil
                   :imports nil
                   :excludes #{}}}

      with-js
      {:cljs "a clojurescript string"
       :js "the emitted JavaScript"
       :forms '(seq-of-cljs-forms)
       :expressions '(seq-of-cljs-expression-objects)
       :namespace {:name com.keminglabs.example.core
                   :doc nil
                   :requires {other com.keminglabs.example.other} :requires-macros {}
                   :uses {foo com.keminglabs.example.other} :uses-macros nil
                   :imports nil
                   :excludes #{}}}))

;;With mulitple compilation maps in hand, we need to put them in dependency order and then optimize with Google Closure
(->> compilation-maps
     dependency-sort ;;can throw ex-info with errors about circular dependencies or unmet dependencies
     google-closure-optimization
     (spit "my-output.js"))