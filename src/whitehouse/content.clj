(ns whitehouse.content
  "Phase 0 seed content.  In later phases this is replaced by the Postgres-backed
   post store; the shapes returned here are the contract the views rely on."
  (:require [markdown.core :as md]))

(def ^:private seed-posts
  "Newest first.  :body is markdown; it is rendered to HTML on read."
  [{:slug    "midsummer-walk-on-the-downs"
    :title   "Midsummer walk on the Downs"
    :date    "2026-06-21"
    :tags    ["walks" "photos"]
    :body    "We took the long route over the Downs for the longest day of the
year. The skylarks were out in force and we stopped for a flask of tea near the
old beacon.

A few things we learned:

- The bridleway past the farm is properly overgrown now — bring secateurs.
- The cafe at the bottom does an excellent cheese scone.
- Everyone slept very well that night."}

   {:slug    "grans-ninetieth"
    :title   "Gran's ninetieth"
    :date    "2026-05-04"
    :tags    ["family" "celebrations"]
    :body    "Ninety years and still the sharpest card player in the family.

We got everyone together for the afternoon — four generations in one room, which
hasn't happened in a long while. Video and a few of the speeches to follow once
I've had a chance to edit them down."}

   {:slug    "first-attempt-at-sourdough"
    :title   "First attempt at sourdough"
    :date    "2026-04-12"
    :tags    ["cooking"]
    :body    "The starter is alive and frankly a little out of control.

Loaf number one was dense as a brick but the second one actually had a crumb you
could be proud of. Recipe and timings saved here so I stop forgetting them."}])

(defn- render [post]
  (assoc post :html (md/md-to-html-string (:body post))))

(defn all-posts
  "All posts, newest first, with rendered HTML."
  []
  (map render seed-posts))

(defn post-by-slug [slug]
  (some #(when (= slug (:slug %)) %)
        (all-posts)))

(defn all-tags
  "Distinct tags with a post count, sorted by name."
  []
  (->> (mapcat :tags seed-posts)
       frequencies
       (sort-by key)
       (map (fn [[tag n]] {:name tag :count n}))))
