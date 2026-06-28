# Whitehouse Family Site — Project Outline

A private, invite-only family site for sharing posts, video, and audio in a
blog-style format. This document is the high-level guardrail for how the site
should evolve. When a change feels like it conflicts with something here, stop
and revisit this document first.

---

## 1. Purpose

A calm, private place for the family to post and read updates — text, photos,
video, and audio — in a dated, blog-style feed. It should feel personal and
last for years, not be a product chasing growth.

**Primary user:** Andrew (author/admin).
**Audience:** family members with `@whitehouse.org.uk` Google Workspace accounts.

---

## 2. Guiding principles

These are the tie-breakers when a decision is unclear.

1. **Private by default.** Nothing is world-readable. Every page and every piece
   of media requires an authenticated, authorised family member.
2. **Built to outlive trends.** Prefer boring, durable technology over the new
   and clever. The site should still run with minimal changes in five years.
3. **Read-optimised.** Most traffic is reading. Pages should be fast, quiet, and
   legible — the Simon Willison reading experience is the bar.
4. **Low operational burden.** One person should be able to run, deploy, and
   reason about the whole thing. Managed services over self-hosting.
5. **Own the data.** Content and media live in services we control and can
   export. No lock-in that makes leaving hard.
6. **Small surface area.** Add features only when wanted. Every feature is a
   maintenance cost forever.

---

## 3. Architecture guardrails

The shape of the system. Changes that cross these lines deserve an explicit
decision (see §8).

- **Host:** Scalingo (EU PaaS). Treat the filesystem as **ephemeral** — never
  persist anything to local disk.
- **App:** Clojure on the JVM. Ring + Jetty, Reitit routing, Hiccup rendering.
  Built to an uberjar.
- **Database:** Scalingo managed PostgreSQL. All structured data and content
  lives here. Schema changes go through migrations (Migratus) — never ad hoc.
- **Media:** S3-compatible object storage. Large files **never** pass through
  the app dyno on upload (pre-signed PUT) and are served privately via
  short-lived pre-signed GET URLs.
- **Auth:** Google OAuth2, restricted to the `whitehouse.org.uk` Workspace
  domain. Sessions are stateless (encrypted cookie) so any dyno can serve any
  request.
- **Statelessness:** The app holds no important state in memory or on disk.
  Restarting or redeploying must never lose data.
- **Config via environment.** Secrets and environment-specific values come from
  env vars, never committed to the repo.

---

## 4. Roadmap

Phased so each stage is usable on its own. Later phases are intentionally vague —
they are direction, not commitment.

### Phase 0 — Skeleton (look & feel)
Running app, styled base layout, home feed with seed data. No DB or auth yet.
Goal: agree the visual design early.

### Phase 1 — Content core
Postgres + migrations. Post model (markdown → HTML), permalinks, dated home
feed, tags, archive pages. Admin-only authoring with a markdown editor.

### Phase 2 — Access control
Google OAuth, `whitehouse.org.uk` domain gate, admin vs. reader roles. The site
becomes genuinely private.

### Phase 3 — Rich media
Pre-signed video/audio/image upload and private playback. Posts can embed media.

### Phase 4 — Family interaction
Comments and emoji reactions for any logged-in family member.

### Phase 5 — Deploy & harden
Scalingo deploy, backups verified, custom domain, monitoring/error reporting.

### Later (candidate, not committed)
Full-text search · email/RSS notifications · multiple authors · photo galleries ·
post drafts/scheduling · per-post visibility (e.g. some posts to a subset).

---

## 5. Scope & non-goals

**In scope:** private family blog, media hosting, comments/reactions, single
admin author.

**Explicit non-goals (for now):**
- Public content or SEO.
- Open sign-up or non-family accounts.
- A mobile app (responsive web only).
- A general-purpose CMS / multi-tenant platform.
- Real-time features (chat, live presence).
- Anything requiring horizontal scale — the audience is a few dozen people.

If a request implies one of these, treat it as a significant direction change,
not a small feature.

---

## 6. Security & privacy

- Every route except the login flow requires an authenticated, authorised user.
- Media URLs are unguessable **and** time-limited; no content is reachable
  without a live session.
- Authorisation is checked server-side on every request, never trusted from the
  client.
- Secrets live only in environment variables and a secret manager — never in
  git, never in client-side code.
- Minimise personal data collected; this is a family site, not an analytics
  product.
- Keep dependencies current for security patches.

---

## 7. Operations

- **Deploy:** push to Scalingo; buildpack builds the uberjar.
- **Migrations:** run on deploy/release, forward-only and reviewed.
- **Backups:** rely on managed Postgres backups; periodically verify a restore.
  Object storage holds the media — confirm its retention/versioning.
- **Observability:** application logs + an error reporter. Keep it simple.
- **Cost:** smallest viable paid tiers; this serves a handful of people. Revisit
  only if usage genuinely grows.

---

## 8. Decision log

Decisions that shaped the project. Add to this when a guardrail changes — note
the date, the decision, and why.

| Date       | Decision | Rationale |
|------------|----------|-----------|
| 2026-06-28 | Host on Scalingo, not Heroku | EU/GDPR-friendly PaaS, Heroku-compatible buildpacks |
| 2026-06-28 | PostgreSQL, not SQLite | Scalingo's filesystem is ephemeral; SQLite would lose data on restart/deploy |
| 2026-06-28 | Media in S3-compatible object storage | Files must not live on the ephemeral dyno; keeps video off the app server |
| 2026-06-28 | Google OAuth restricted to `whitehouse.org.uk` Workspace | Family have Workspace accounts; domain gate is clean and simple |
| 2026-06-28 | Single admin author (Andrew) | Simplest permission model for v1; others are read-only |
| 2026-06-28 | Comments + reactions in v1 | Wanted for family interaction; other features deferred |
| 2026-06-28 | Clojure on the JVM | Author preference; durable, well-suited to a small read-heavy app |

---

*This is a living document. Keep it short, keep it honest, and update it when
reality diverges from the plan.*
