# Whitehouse Family Site

A private, invite-only family blog with photos, video and audio.
See [PROJECT.md](PROJECT.md) for the vision, architecture guardrails, and roadmap.

## Status

**Phase 0 — Skeleton** plus **Phase 2 sign-in.** A running, styled home feed and
post pages over in-memory seed content, gated behind Google sign-in restricted to
the `whitehouse.org.uk` Workspace domain. No database yet.

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

## Sign-in (Google OAuth)

Every page except the login flow requires a signed-in `@whitehouse.org.uk`
Google account. Sessions are stateless — the user's identity lives in an
AES-encrypted cookie, so no server-side session store is needed.

### One-time Google Cloud setup

1. In the [Google Cloud Console](https://console.cloud.google.com/) create (or
   pick) a project.
2. **APIs & Services → OAuth consent screen**: choose **Internal** (this limits
   sign-in to the Workspace org) and fill in the app name.
3. **APIs & Services → Credentials → Create credentials → OAuth client ID**:
   - Application type: **Web application**.
   - **Authorised redirect URIs**: add
     `http://localhost:3000/oauth/callback` for local dev, and your production
     callback (e.g. `https://your-app.osc-fr1.scalingo.io/oauth/callback`) later.
4. Copy the **Client ID** and **Client secret**.

### Configure the app

Copy `.env.example` to `.env` (gitignored) and fill it in:

```bash
cp .env.example .env
```

| Variable               | Required | Notes                                             |
|------------------------|----------|---------------------------------------------------|
| `GOOGLE_CLIENT_ID`     | yes      | From the OAuth client above.                      |
| `GOOGLE_CLIENT_SECRET` | yes      | From the OAuth client above.                      |
| `OAUTH_REDIRECT_URI`   | prod     | Must match a registered redirect URI. Local default is `http://localhost:3000/oauth/callback`. |
| `SESSION_SECRET`       | prod     | Exactly 16 chars; encrypts the session cookie. Without it a random per-restart key is used (dev only). |
| `ALLOWED_DOMAIN`       | no       | Defaults to `whitehouse.org.uk`.                  |
| `ADMIN_EMAILS`         | no       | Comma-separated emails granted the admin role.    |

`.env` is not loaded automatically — export it before running, e.g.
`set -a && source .env && set +a && clj -M:dev`. On Scalingo, set these as
environment variables in the dashboard.

## To do

### To turn sign-in on (now)
- [ ] Create the Google OAuth client (see [Sign-in](#sign-in-google-oauth)).
- [ ] Copy `.env.example` to `.env` and set `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`.
- [ ] Do a real end-to-end sign-in with a `@whitehouse.org.uk` account and confirm
      a non-family Google account is rejected. (Only the flow, not a live Google
      login, has been verified so far.)

### Before deploy
- [ ] Set a fixed 16-char `SESSION_SECRET` in production (otherwise sessions drop
      on every restart).
- [ ] Add the production callback (`https://<app>/oauth/callback`) to the OAuth
      client's authorised redirect URIs and set `OAUTH_REDIRECT_URI` to match.
- [ ] Set `ADMIN_EMAILS` and confirm the intended admin address.
- [ ] Serve over HTTPS so the `Secure` session cookie is honoured.

### Roadmap ahead (see [PROJECT.md](PROJECT.md) §4)
- [ ] **Phase 1 — Content core:** Postgres + Migratus migrations; move posts out
      of `content.clj`; markdown authoring, tags, archive pages.
- [ ] **Phase 2 — remaining:** enforce the admin vs. reader distinction (nothing
      gates on `:admin` yet — it is only captured in the session).
- [ ] **Phase 3 — Rich media:** pre-signed S3 upload and private playback.
- [ ] **Phase 4 — Family interaction:** comments and reactions.
- [ ] **Phase 5 — Deploy & harden:** Scalingo deploy, backups, custom domain,
      monitoring/error reporting.

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
  handler.clj   Routes + session/auth middleware
  auth.clj      Google OAuth2 sign-in, domain gate, session store
  views.clj     Hiccup rendering
  content.clj   Seed content (replaced by Postgres in Phase 1)
resources/
  public/css/   Stylesheet
```
