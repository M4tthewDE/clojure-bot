(ns clojure-bot.core
  (:import [java.net Socket])
  (:require [clojure.java.io :as jio])
  (:require [clojure.string :as str])
  (:require [cheshire.core :as json])
  (:gen-class))

(defrecord Message [msg-type channel user content]
  Object
  (toString [this]
    (str msg-type " in #" channel " by " user (if (> (count content) 0) (str ": " content) ""))))

(defn parse-msg [line]
  (let [parts (str/split line #" ")]
    (map->Message {:msg-type (get parts 1)
                   :channel (-> parts
                                (get 2)
                                (subs 1))
                   :user (-> parts
                             (get 0)
                             (str/split #"!")
                             (get 0)
                             (subs 1))
                   :content (if (> (count parts) 3)
                              (as-> parts p
                                (drop 3 p)
                                (str/join " " p)
                                (subs p 1))
                              "")})))

(defn parse [line, username]
  (cond (str/starts-with? line ":tmi.twitch.tv") nil
        (str/starts-with? line (str ":" username ".tmi.twitch.tv")) nil
        (str/starts-with? line "PING") (->Message "PING" "" "" "")
        :else (parse-msg line)))

(defn send-raw [writer, msg]
  (do
    (.write writer (str msg "\r\n"))
    (.flush writer)))

(defn send-privmsg [writer, channel, content]
  (send-raw writer (str "PRIVMSG #" channel " :" content)))

(defrecord Command [cmd-name]
  Object
  (toString [this]
    (str "command '" cmd-name "'")))

(defn parse-command [content]
  (if (str/starts-with? content "!")
    (-> content
        (str/split #" ")
        (get 0)
        (subs 1)
        ->Command)
    nil))

(defn ping-cmd [writer channel]
  (send-privmsg writer channel "PONG!"))

(defn handle-cmd [cmd, writer, channel]
  (case (:cmd-name cmd)
    "ping" (ping-cmd writer channel)
    (println (str "[cmd] Unrecognized command '" (:cmd-name cmd) "'"))))

(defn handle-msg [msg, writer]
  (case (:msg-type msg)
    "PING" (do
             (println "[reader] Answering PING with PONG")
             (send-raw writer "PONG :tmi.twitch.tv"))
    "JOIN" (send-privmsg writer (:channel msg) (str "Joined #" (:channel msg)))
    (if-let [cmd (parse-command (:content msg))]
      (do
        (println (str "[cmd] Handling " cmd))
        (handle-cmd cmd writer (:channel msg))))))

(defn read-loop [reader, writer, username]
  (loop []
    (let [line (.readLine reader)]
      (if line
        (do
          (if-let [msg (parse line username)]
            (do
              (println (str "[reader] " msg))
              (handle-msg msg writer)))
          (recur))
        (println "No more lines in reader.")))))

(defn join [writer, config]
  (send-raw writer (str "PASS oauth:" (get config "token")))
  (send-raw writer (str "NICK " (get config "username")))
  (send-raw writer (str "JOIN #" (get config "channel"))))

(defn run [config]
  (with-open [socket (Socket. "irc.chat.twitch.tv" 6667)]
    (with-open [writer (jio/writer socket)]
      (join writer config)
      (with-open [reader (jio/reader socket)]
        (read-loop reader writer (get config "username"))))))

(defn read-config []
  (json/parse-string (slurp "config.json")))

(defn -main
  [& args]
  (run (read-config)))

