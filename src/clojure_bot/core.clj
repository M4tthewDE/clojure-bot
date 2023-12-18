(ns clojure-bot.core
  (:import [java.net Socket])
  (:require [clojure.java.io :as jio])
  (:require [clojure.string :as str])
  (:gen-class))

(defrecord Message [msg-type channel user content])

(defn parse_msg [line]
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
        :else (parse_msg line)))

(defn handle_msg [msg, writer]
  (case (:msg-type msg)
    "PING" (do
             (println "Answering PING with PONG")
             (.write writer "PONG :tmi.twitch.tv\r\n")
             (.flush writer))
    (println msg)))

(defn read_loop [reader, writer]
  (loop []
    (let [line (.readLine reader)]
      (if (nil? line)
        (do
          (println "No more lines in reader."))
        (do
          (let [msg (parse line)]
            (when msg (handle_msg msg writer)))
          (recur))))))

(defn join [writer]
  (.write writer "PASS oauth: \r\n")
  (.write writer "NICK justinfan6969\r\n")
  (.write writer "JOIN #matthewde\r\n")
  (.flush writer))

(defn run
  "Starts the bot."
  []
  (with-open [socket (Socket. "irc.chat.twitch.tv" 6667)]
    (with-open [writer (jio/writer socket)]
      (join writer)
      (with-open [reader (jio/reader socket)]
        (read_loop reader writer)))))

(defn -main
  [& args]
  (run))

