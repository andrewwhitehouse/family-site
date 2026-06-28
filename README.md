# Whitehouse Family Site

A private, invite-only family blog with photos, video and audio.
See [PROJECT.md](PROJECT.md) for the vision, architecture guardrails, and roadmap.

## Status

**Phase 0 — Skeleton.** A running, styled home feed and post pages over in-memory
seed content. No database or authentication yet.

## Stack

Clojure (JVM) · Ring + Jetty · Reitit · Hiccup rendering · Postgres + object
storage (later phases) · deployed to Scalingo.

## Run locally

Requires Java 21+ and the [Clojure CLI](https://clojure.org/guides/install_clojure).

```bash
clj -M:dev
```

Then open http://localhost:3000.

Set `PORT` to use a different port:

```bash
PORT=8080 clj -M:dev
```

## Build a deployable uberjar

```bash
clj -T:build uber      # produces target/app.jar
java -jar target/app.jar
```

This is what Scalingo runs via the `Procfile`.

## Layout

```
src/whitehouse/
  server.clj    Jetty entry point (-main)
  handler.clj   Routes
  views.clj     Hiccup rendering
  content.clj   Seed content (replaced by Postgres in Phase 1)
resources/
  public/css/   Stylesheet
```
