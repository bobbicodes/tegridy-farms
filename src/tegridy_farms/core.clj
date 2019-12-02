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
  (try
    (client/get "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
                {:oauth-token github-oauth-token})
    (catch Exception e (println (str "http error: " e)))))

;; get a single pull request
;; GET /repos/:owner/:repo/pulls/:pull_number

(defn get-pr [n]
  (try
    (client/get (str "https://api.github.com/repos/porkostomus/tegridy-farms/pulls/" n)
                {:oauth-token github-oauth-token})
    (catch Exception e (println (str "Caught exception: " e)))))

(defn tweet [msg]
  (try 
    (rest/statuses-update :oauth-creds twitter-creds :params {:status msg})
    (catch Exception e (println (str "Caught exception: " e)))))

(defn latest-tweet []
  (try
    (rest/statuses-user-timeline :oauth-creds twitter-creds :params {:count 1})
    (catch Exception e (println (str "Caught exception: " e)))))

(defn char-limit [n s]
  (if (< n (count s))
    (str (apply str (take n s)) "...")
    s))

(defn pr-summary [pr]
  (let [{:keys [number title body html_url]} pr]
    (str "PR #" number ": " (char-limit 60 title) "\n"
         (char-limit 180 body)
         html_url)))

(defn last-pr-tweeted
  "Returns the number of the most recently posted PR"
  []
  (let [pr-num (re-find #"PR #\d+"  (:text (first (:body (latest-tweet)))))]
    (Integer/parseInt (subs pr-num 4))))

(defn new-pr-nums
  "Returns list of PR numbers on Github"
  []
  (map :number
       (json/read-str
        (:body (pr-list))
        :key-fn keyword)))

(defn untweeted-prs []
  (filter #(< (last-pr-tweeted) %) (new-pr-nums)))

(defn -main []
  (if-let [tweets (seq (untweeted-prs))]
    (let [next-tweet (first (sort tweets))]
      (println (str "Tweeting PR #" next-tweet " ..."))
      (tweet (pr-summary (json/read-str (:body (get-pr next-tweet)) :key-fn keyword)))
      (println "Will check for more in 1 minute..."))
    (println "No PRs to tweet. Will try again in 1 minute..."))
  (Thread/sleep 60000)
  (-main))

(comment
  (-main)
  (last-pr-tweeted)
  (sort (untweeted-prs))
 (tweet (pr-summary (json/read-str (:body (get-pr (first (sort (untweeted-prs))))) :key-fn keyword)))
 (Integer/parseInt (str (nth (:text (first (:body (latest-tweet)))) 4)))
 (tweet (pr-summary (first (json/read-str
                                   (:body (pr-list))
                                   :key-fn keyword))))
  )
