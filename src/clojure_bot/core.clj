(ns clojure-bot.core
  (:import [java.net Socket])
  (:require [clojure.java.io :as jio])
  (:require [clojure.string :as str])
  (:require [cheshire.core :as json])
  (:gen-class))

(defrecord Message [msg-type channel user content]
  Object
  (toString [this]
    (str "msg-type: " msg-type ", channel: " channel ", user: " user ", content: " content)))

(defn parse-msg [line]
  (let [parts (str/split line #" ")]
    (map->Message {:msg-type (get parts 1)
                   :channel (subs (get parts 2) 1)
                   :user (subs (get (str/split (get parts 0) #"!") 0) 1)
                   :content (if (> (count parts) 3)
                              (subs (str/join " " (drop 3 parts)) 1) "")})))

(defn parse [line, username]
  (cond (str/starts-with? line ":tmi.twitch.tv") nil
        (str/starts-with? line (str ":" username ".tmi.twitch.tv")) nil
        (str/starts-with? line "PING") (->Message "PING" "" "" "")
        :else (parse-msg line)))

(defn send-raw [writer, msg]
  (do
    (.write writer (str msg "\r\n"))
    (.flush writer)))

(defn handle-msg [msg, writer]
  (case (:msg-type msg)
    "PING" (do
             (println "[reader] Answering PING with PONG")
             (send-raw writer "PONG :tmi.twitch.tv"))
    (println (str "[reader] " msg))))

(defn read-loop [reader, writer, username]
  (loop []
    (let [line (.readLine reader)]
      (if line
        (do
          (if-let [msg (parse line username)]
            (handle-msg msg writer))
          (recur))
        (println "No more lines in reader.")))))

(defn join [writer, token, username]
  (send-raw writer (str "PASS oauth:" token))
  (send-raw writer (str "NICK " username))
  (send-raw writer "JOIN #matthewde"))

(defn run [config]
  (with-open [socket (Socket. "irc.chat.twitch.tv" 6667)]
    (with-open [writer (jio/writer socket)]
      (join writer (get config "token") (get config "username"))
      (with-open [reader (jio/reader socket)]
        (read-loop reader writer (get config "username"))))))

(defn read-config []
  (json/parse-string (slurp "config.json")))

(defn -main
  [& args]
  (run (read-config)))

