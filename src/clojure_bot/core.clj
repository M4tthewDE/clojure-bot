(ns clojure-bot.core
  (:import [java.net Socket])
  (:require [clojure.java.io :as jio])
  (:require [clojure.string :as str])
  (:gen-class))

;; parser
(defrecord Message [msg-type channel user content])

(defn parse-msg [line]
  (let [parts (str/split line #" ")]
    (->Message
     (get parts 1)
     (subs (get parts 2) 1)
     (subs (get (str/split (get parts 0) #"!") 0) 1)
     (subs (str/join " " (drop 3 parts)) 1))))

(defn parse [line]
  (cond (str/starts-with? line ":tmi.twitch.tv") nil
        (str/starts-with? line ":justinfan6969") nil
        (str/starts-with? line "PING") (->Message "PING" "" "" "")
        :else (parse-msg line)))

;; sending
(defn send-raw [writer, msg]
  (do
    (.write writer (str msg "\r\n"))
    (.flush writer)))

;; reading
(defn handle-msg [msg, writer]
  (case (:msg-type msg)
    "PING" (do
             (println "Answering PING with PONG")
             (send-raw writer "PONG :tmi.twitch.tv"))
    (println msg)))

(defn read-loop [reader, writer]
  (loop []
    (let [line (.readLine reader)]
      (if line
        (do
          (if-let [msg (parse line)]
            (handle-msg msg writer))
          (recur))
        (println "No more lines in reader.")))))

;; main
(defn join [writer]
  (send-raw writer "PASS oauth: ")
  (send-raw writer "NICK justinfan6969")
  (send-raw writer "JOIN #matthewde"))

(defn run
  []
  (with-open [socket (Socket. "irc.chat.twitch.tv" 6667)]
    (with-open [writer (jio/writer socket)]
      (join writer)
      (with-open [reader (jio/reader socket)]
        (read-loop reader writer)))))

(defn -main
  [& args]
  (run))

