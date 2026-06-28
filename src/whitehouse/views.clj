(ns whitehouse.views
  "Selmer template rendering."
  (:require [selmer.parser :as selmer]
            [selmer.util :as selmer-util]))

;; Treat a missing template variable as an error in dev rather than silently
;; rendering nothing.
(selmer-util/set-missing-value-formatter!
 (fn [tag _context] (str "<!-- missing: " (:tag-value tag) " -->")))

(def ^:private site
  {:title "The Whitehouse Family"
   :tagline "Private family posts, photos, video and audio."})

(defn render
  "Render a template under resources/templates with the shared site context."
  [template ctx]
  (selmer/render-file (str "templates/" template)
                      (merge {:site site} ctx)))

(defn home [{:keys [posts tags]}]
  (render "home.html" {:posts posts :tags tags}))

(defn post [{:keys [post tags]}]
  (render "post.html" {:post post :tags tags}))

(defn not-found []
  (render "not_found.html" {}))
