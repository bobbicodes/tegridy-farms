(ns tegridy-farms.core
  (:require [clj-http.client :as client]))

;; poll a GitHub repo, look for new pull requests
;; GET /repos/:owner/:repo/pulls

(defn pr-list []
  (client/get "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
              {:oauth-token (slurp "config.edn")}))


;; Create a pull request
;; POST /repos/:owner/:repo/pulls

(defn create-pr []
  (client/post "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
              {:oauth-token (slurp "config.edn")
               :body        "{
  \"title\": \"Amazing new feature\",
  \"body\": \"Please pull this in!\",
  \"head\": \"create-pr\",
  \"base\": \"master\"
}"}))

(comment
  
  (slurp "config.edn")
  (pr-list)
  (create-pr)
  )