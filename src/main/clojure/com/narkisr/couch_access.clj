(ns com.narkisr.couch-access
  (:refer-clojure :exclude [contains?])
  (:require [clojure.http.resourcefully :as resourcefully])
  (:use
    (com.narkisr fs-logic)
    (couchdb (client :only [document-list document-get view-get]))
    (clojure.contrib (str-utils2 :only [contains?]) error-kit (def :only [defn-memo]))
    (clojure.contrib.json read write)))

(def *host* "http://127.0.0.1:5984/")
(def *db* "fuse")

(defn couch [fn & params]
  "Applies the binded couch configuration on the given fn, this cannot be a macro since the values are binded post the expansion stage"
  (apply fn *host* *db* params))

(defn- not-design-id [id]
  (not (contains? id "design")))

(defn- all-ids []
  (lazy-seq (filter not-design-id (couch document-list))))

(defn- create-file-entry [name]
  {name (create-node file name 0644 [:description "A couch doc" :mimetype "text/plain"] #(-> (str *host* *db* "/" name) resourcefully/get :body-seq first (. getBytes)))})

(defn couch-files []
  (reduce merge (map #(create-file-entry %) (all-ids))))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn fetch-size [file]
  "Fetches file size in bytes using http HEAD, note that size is + 1 more than the actual content size."
  (-> (str *host* *db* "/" (:name file)) resourcefully/head (get-in [:headers :content-length]) first Integer/parseInt (- 1)))

(defn db-exists? [host db]
 (try (resourcefully/get (str host db))
   (catch java.io.IOException e nil)))
