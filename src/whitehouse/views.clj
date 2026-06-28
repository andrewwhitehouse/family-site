(ns whitehouse.views
  "Hiccup HTML rendering."
  (:require [hiccup2.core :as h]
            [hiccup.util :refer [raw-string]]))

(def ^:private site
  {:title "The Whitehouse Family"
   :tagline "Private family posts, photos, video and audio."})

(defn- page-title
  "Page <title>, suffixed with the site name."
  [s]
  (str s " — " (:title site)))

(defn- post-path [post]
  (str "/posts/" (:slug post)))

(defn- tag-links
  "Inline tag chips as used in post metadata."
  [tags]
  (for [tag tags]
    [:a.tag {:href (str "/tags/" tag)} tag]))

(defn- post-meta
  "Date and tag chips shown beneath a post heading."
  [post]
  [:p.post-meta
   [:time {:datetime (:date post)} (:date post)]
   (tag-links (:tags post))])

(defn- post-body [post]
  [:div.post-body (raw-string (:html post))])

(def ^:private back-link
  [:p.back [:a {:href "/"} "← Back to all posts"]])

(defn- sidebar [tags]
  (list
   [:section.sidebar-block
    [:h2 "Tags"]
    [:ul.tag-list
     (for [{:keys [name count]} tags]
       [:li [:a {:href (str "/tags/" name)} name] " "
        [:span.muted count]])]]
   [:section.sidebar-block
    [:h2 "About"]
    [:p.muted "A private place for the family to share news, photos, video
     and audio. Sign-in required."]]))

(defn- layout
  "Wrap page content in the shared document chrome."
  [{:keys [title content sidebar]}]
  (str
   "<!DOCTYPE html>\n"
   (h/html
    {:mode :html}
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:meta {:name "robots" :content "noindex, nofollow"}]
      [:title (or title (:title site))]
      [:link {:rel "stylesheet" :href "/css/style.css"}]]
     [:body
      [:header.site-header
       [:div.wrap
        [:a.site-title {:href "/"} (:title site)]
        [:p.site-tagline (:tagline site)]]]
      [:main.wrap.layout
       [:div.content content]
       [:aside.sidebar sidebar]]
      [:footer.site-footer
       [:div.wrap
        [:p "A private family site. Please don't share links outside the family."]]]]])))

(defn home [{:keys [posts tags]}]
  (layout
   {:content
    [:div.feed
     (for [post posts]
       [:article.post-summary
        [:header
         [:h2 [:a {:href (post-path post)} (:title post)]]
         (post-meta post)]
        (post-body post)
        [:p.read-more [:a {:href (post-path post)} "Permalink"]]])]
    :sidebar (sidebar tags)}))

(defn post [{:keys [post tags]}]
  (layout
   {:title (page-title (:title post))
    :content
    [:article.post-full
     [:header
      [:h1 (:title post)]
      (post-meta post)]
     (post-body post)
     back-link]
    :sidebar (sidebar tags)}))

(defn not-found []
  (layout
   {:title (page-title "Not found")
    :content
    [:article.post-full
     [:h1 "Not found"]
     [:p "That page doesn't exist, or has moved."]
     back-link]}))
