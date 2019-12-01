(ns tegridy-farms.core
  (:require [clojure.edn :as edn]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [next.jdbc :as jdbc]
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

(defn latest-tweet []
  (rest/statuses-user-timeline :oauth-creds twitter-creds :params {:count 1}))

(defn char-limit [n s]
  (if (< n (count s))
    (str (apply str (take n s)) "...")
    s))

(defn pr-summary [pr]
  (let [{:keys [number title body html_url]} pr]
    (str "PR #" number ": " (char-limit 60 title) "\n"
         (char-limit 180 body)
         html_url)))

(def db {:dbtype "postgresql"
         :dbname "tegridy"
         :user "postgres"
         :password "pwd"})

(def ds (jdbc/get-datasource db))

(defn create-requests-table! []
  (jdbc/execute! ds ["
create table requests (
  id serial primary key,
  title varchar (255) not null,
  body varchar (255) not null,
  url varchar (255) not null
)"]))

;; need to escape single-quotes

(defn insert-request [request]
  (let [{:keys [title body html_url]} request]
    (jdbc/execute! ds [(str "
insert into requests(title, body, url)
  values('" (char-limit 250 title) "', '" (char-limit 250 body) "', '" html_url "')")])))

(defn last-pr-tweeted
  "Returns the number of the most recently posted PR"
  []
  (Integer/parseInt (str (nth (:text (first (:body (latest-tweet)))) 4))))

(comment
  (= 7 (last-pr-tweeted))
  
 (Integer/parseInt (str (nth (:text (first (:body (latest-tweet)))) 4)))
 (tweet (pr-summary (first (json/read-str
                                     (:body (pr-list))
                                     :key-fn keyword))))
  (insert-request (json/read-str (:body (get-pr 8)) :key-fn keyword))
  (jdbc/execute! ds ["select * from requests"])
  )