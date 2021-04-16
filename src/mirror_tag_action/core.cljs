(ns mirror-tag-action.core
  (:require [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            ["@actions/core" :as core]
            ["@actions/github" :as github]))

(defn start []
  (go
   (try
     (let [gh-token (.getInput core "github-token")
           dest-branch (.getInput core "dest")
           octokit (.getOctokit github gh-token)
           ref (.. js/process -env -GITHUB_REF)
           tag (second (re-find #"refs/tags/([^&]*)" ref))
           repo-context (js->clj (.. github -context -repo))
           ref-args (-> repo-context
                        (merge {:ref (str "tags/" tag)})
                        clj->js)
           _ (println {:dest-branch dest-branch
                       :ref ref
                       :tag tag})
           _ (println ref-args)
           ref-resp (js->clj (<p! (.. octokit -rest -git (getRef ref-args))))
           _ (println ref-resp)
           _ (prn ref-resp)
           ref-type (get-in ref-resp ["data" "object" "type"])
           sha (get-in ref-resp ["data" "object" "sha"])
           _ (println ref-type)
           _ (println sha)
           update-args (-> repo-context
                           (merge {:ref (str "refs/heads/" dest-branch)
                                   :sha sha
                                   :force true})
                           clj->js)]

       (prn update-args)

       (when (not= "commit" ref-type)
         (throw (js/Error. "Expected ref to be a commit. Got a " ref-type)))

       (println (str "Pushing tag " tag " (" sha ") to branch " dest-branch))

       (prn octo)

       (try
         (println (<p! (.. octokit -rest -git (updateRef update-args))))
         (catch :default e
           (prn e)
           (prn (.-message e))
           (if (= (.-message e) "Reference does not exist")
             (do
               (prn (str "Branch " dest-branch "does not exist. Creating it."))
               (prn (<p! (.. octokit -rest -git (createRef update-args)))))
             (throw (js/Error.)))))

       (println (str "Set branch " dest-branch " to " sha)))
     (catch :default e
       (.setFailed core e)))))

(defn -main []
  (start))
