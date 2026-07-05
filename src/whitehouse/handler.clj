(ns whitehouse.handler
  "HTTP routes. Public content is now gated behind Google sign-in restricted to
   the family Workspace domain (see whitehouse.auth)."
  (:require [reitit.ring :as ring]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as resp]
            [whitehouse.auth :as auth]
            [whitehouse.content :as content]
            [whitehouse.views :as views]))

(defn- html [body]
  (-> (resp/response body)
      (resp/content-type "text/html; charset=utf-8")))

(defn home-handler [request]
  (html (views/home {:posts (content/all-posts)
                     :tags  (content/all-tags)
                     :user  (auth/current-user request)})))

(defn post-handler [{{:keys [slug]} :path-params :as request}]
  (if-let [post (content/post-by-slug slug)]
    (html (views/post {:post post
                       :tags (content/all-tags)
                       :user (auth/current-user request)}))
    (-> (html (views/not-found {:user (auth/current-user request)})) (resp/status 404))))

(def router
  (ring/ring-handler
   (ring/router
    [["/"               {:get home-handler}]
     ["/posts/:slug"    {:get post-handler}]
     ["/login"          {:get auth/login}]
     ["/oauth/start"    {:get auth/start}]
     ["/oauth/callback" {:get auth/callback}]
     ["/logout"         {:get auth/logout}]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (fn [request]
                   (-> (html (views/not-found {:user (auth/current-user request)}))
                       (resp/status 404)))}))))

(def app
  (-> router
      auth/wrap-require-auth
      wrap-params
      (wrap-session (auth/session-options))))
