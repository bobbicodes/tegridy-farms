(ns tegridy-farms.core
  (:require [clj-http.client :as client]))

;; poll a GitHub repo, look for new pull requests
;; GET /repos/:owner/:repo/pulls

(defn pr-list []
  (client/get "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
              {:oauth-token (slurp "config.edn")}))

(comment
  
  (slurp "config.edn")
  (pr-list)
  )