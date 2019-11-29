(ns tegridy-farms.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

;; poll a GitHub repo, look for new pull requests
;; GET /repos/:owner/:repo/pulls

(defn pr-list []
  (client/get "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
              {:oauth-token (slurp "config.edn")}))

;; get a single pull request
;; GET /repos/:owner/:repo/pulls/:pull_number

(defn get-pr [n]
  (client/get (str "https://api.github.com/repos/porkostomus/tegridy-farms/pulls/" n)
              {:oauth-token (slurp "config.edn")}))

(comment
 (keys (first (json/read-str 
               (:body (pr-list))
               :key-fn keyword)))
 (json/read-str (:body (get-pr 1)) :key-fn keyword)
 )