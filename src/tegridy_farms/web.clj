(ns tegridy-farms.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [twitter.oauth :as oauth]
            [twitter.api.restful :as rest]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [hiccup.page :as page]))

(def github-oauth-token (env :github-oauth-token))

(def twitter-creds
    (oauth/make-oauth-creds (env :twitter-api-key) (env :twitter-api-secret-key) (env :twitter-access-token) (env :twitter-access-token-secret)))

(defn pr-list []
  (try
    (client/get "https://api.github.com/repos/porkostomus/tegridy-farms/pulls"
                {:oauth-token github-oauth-token})
    (catch Exception e (println (str "Error retrieving PRs: " e)))))

(defn get-pr [n]
  (try
    (client/get (str "https://api.github.com/repos/porkostomus/tegridy-farms/pulls/" n)
                {:oauth-token github-oauth-token})
    (catch Exception e (println (str "Error retrieving PR: " e)))))

(defn latest-tweet []
  (try
    (rest/statuses-user-timeline :oauth-creds twitter-creds :params {:count 1})
    (catch Exception e (println (str "Error retreiving tweet: " e)))))

(defn tweet [msg]
  (try
    (rest/statuses-update :oauth-creds twitter-creds :params {:status msg})
    (catch Exception e (println (str "Error sending tweet: " e)))))

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
  "Returns list of numbers of PRs on Github"
  []
  (map :number
       (json/read-str
        (:body (pr-list))
        :key-fn keyword)))

(defn untweeted-prs []
  (filter #(< (last-pr-tweeted) %) (new-pr-nums)))

(defn sync-tweets []
  (if-let [tweets (seq (untweeted-prs))]
    (let [next-tweet (first (sort tweets))]
      (str "Tweeting PR #" next-tweet " ...")
      (tweet (pr-summary (json/read-str (:body (get-pr next-tweet)) :key-fn keyword)))))
  (str "No PRs to tweet."))

(defn index []
  (page/html5
   [:head
    [:title "Farming with Tegridy"]]
   [:body
    [:div {:id "content"} 
     [:h1 "Farming with Tegridy"]
     [:p (sync-tweets)]]]))

(defroutes app
  (GET "/" []
       (index))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
