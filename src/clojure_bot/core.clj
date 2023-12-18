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
        :else (parse_msg line)))

(defn read_loop [reader]
  (loop []
    (let [line (.readLine reader)]
      (if (nil? line)
        (do
          (println "No more lines in reader."))
        (do
        ;; TODO answer PING with PONG
          (let [msg (parse line)]
            (when msg (println msg)))
          (recur))))))

(defn join [writer]
  (.write writer "PASS oauth: \n\r")
  (.write writer "NICK justinfan6969\n\r")
  (.write writer "JOIN #matthewde\n\r")
  (.flush writer))

(defn run
  "Starts the bot."
  []
  (with-open [socket (Socket. "irc.chat.twitch.tv" 6667)]
    (with-open [writer (jio/writer socket)]
      (join writer)
      (with-open [reader (jio/reader socket)]
        (read_loop reader)))))

(defn -main
  [& args]
  (run))

