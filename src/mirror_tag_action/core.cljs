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
           tag (second (re-find #"/refs/tags/([^&]*)" ref))
           repo-context (js->clj (.. github -context -repo))
           ref-args (-> repo-context
                        (merge {:ref (str "tags/" tag)})
                        clj->js)
           ref-resp (js->clj (<p! (.. octokit -git (getRef ref-args))))
           ref-type (get-in ref-resp ["data" "object" "type"])
           sha (get-in ref-resp ["data" "object" "sha"])
           update-args (-> repo-context
                           (merge {:ref (str "refs/heads/" dest-branch)
                                   :sha sha
                                   :force true})
                           clj->js)]

       (when (not= "tag" ref-type)
         (throw (js/Error. "Expected ref to be a tag. Got a " ref-type)))

       (println (str "Pushing tag " tag " (" sha ") to branch " dest-branch))

       (try
         (<p! (.. octokit -git (updateRef update-args)))
         (catch :default e
           (if (= e "Reference does not exist")
             (do
               (println (str "Branch " dest-branch "does not exist. Creating it."))
               (<p! (.. octokit -git (createRef update-args))))
             (throw (js/Error.)))))

       (println (str "Set branch " dest-branch " to " sha)))
     (catch :default e
       (.setFailed core e)))))

(defn -main []
  (start))
