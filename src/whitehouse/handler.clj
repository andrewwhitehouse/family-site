(ns whitehouse.handler
  "HTTP routes.  Phase 0: public read-only views over seed content."
  (:require [reitit.ring :as ring]
            [ring.util.response :as resp]
            [whitehouse.content :as content]
            [whitehouse.views :as views]))

(defn- html [body]
  (-> (resp/response body)
      (resp/content-type "text/html; charset=utf-8")))

(defn home-handler [_]
  (html (views/home {:posts (content/all-posts)
                     :tags  (content/all-tags)})))

(defn post-handler [{{:keys [slug]} :path-params}]
  (if-let [post (content/post-by-slug slug)]
    (html (views/post {:post post
                       :tags (content/all-tags)}))
    (-> (html (views/not-found)) (resp/status 404))))

(def app
  (ring/ring-handler
   (ring/router
    [["/"            {:get home-handler}]
     ["/posts/:slug" {:get post-handler}]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (fn [_] (-> (html (views/not-found)) (resp/status 404)))}))))
