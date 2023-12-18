(ns clojure-bot.core
  (:import [java.net Socket])
  (:require [clojure.java.io :as jio])
  (:gen-class))

(defn read_loop [reader]
  (loop []
    (let [line (.readLine reader)]
      (if (nil? line)
        (do
          println "End of stream reached.")
      (do
        (println line)
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

