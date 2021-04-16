(ns mirror-tag-action.core
  (:require [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            ["@actions/core" :as core]
            ["@actions/github" :as github]))

;; create wrapper fns for octokit methods to get around externs issues
(defn get-ref [^js octokit args] (.. octokit -git (getRef args)))
(defn update-ref [^js octokit args] (.. octokit -git (updateRef args)))
(defn create-ref [^js octokit args] (.. octokit -git (createRef args)))

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
           ref-resp (js->clj (<p! (get-ref octokit ref-args)))
           ref-type (get-in ref-resp ["data" "object" "type"])
           sha (get-in ref-resp ["data" "object" "sha"])
           update-args (-> repo-context
                           (merge {:ref (str "refs/heads/" dest-branch)
                                   :sha sha
                                   :force true})
                           clj->js)]

       (when (not= "commit" ref-type)
         (throw (js/Error. "Expected ref to be a commit. Got a " ref-type)))

       (println (str "Pushing tag " tag " (" sha ") to branch " dest-branch))

       (try
         (<p! (update-ref octokit update-args))
         (catch :default e
           (if (= (.. e -cause -message) "Reference does not exist")
             (do
               (println (str "Branch " dest-branch "does not exist. Creating it."))
               (<p! (create-ref octokit update-args)))
             (throw (js/Error.)))))

       (println (str "Set branch " dest-branch " to " sha)))
     (catch :default e
       (.setFailed core e)))))

(defn -main []
  (start))
