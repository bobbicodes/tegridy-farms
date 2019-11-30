(ns tegridy-farms.core
  (:require [clojure.edn :as edn]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [twitter.oauth :as oauth]
            [twitter.api.restful :as rest]))

;; poll a GitHub repo, look for new pull requests
;; GET /repos/:owner/:repo/pulls

(def github-oauth-token (:oauth-token (:github (edn/read-string (slurp "config.edn")))))

(def twitter-creds
  (let [{:keys [api-key api-secret-key access-token access-token-secret]} (:twitter (edn/read-string (slurp "config.edn")))]
    (oauth/make-oauth-creds api-key api-secret-key access-token access-token-secret)))

(defn pr-list []
  (client/get "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
              {:oauth-token github-oauth-token}))

;; get a single pull request
;; GET /repos/:owner/:repo/pulls/:pull_number

(defn get-pr [n]
  (client/get (str "https://api.github.com/repos/porkostomus/tegridy-farms/pulls/" n)
              {:oauth-token github-oauth-token}))

(defn tweet [msg]
  (rest/statuses-update :oauth-creds twitter-creds :params {:status msg}))

(defn pr-summary [pr]
  (let [{:keys [number title body html_url]} pr]
    (str "PR#" number " 
         " "Title: " title " 
         " "Body: " body " 
         " "URL: " html_url)))

(comment
 (tweet (pr-summary (first (json/read-str 
                            (:body (pr-list))
                            :key-fn keyword))))
 (keys (json/read-str (:body (get-pr 1)) :key-fn keyword))
  (tweet "Testing simpler creds function.")
 )