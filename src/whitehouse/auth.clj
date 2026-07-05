(ns whitehouse.auth
  "Google OAuth2 sign-in, restricted to the family Workspace domain.

   Identity is stateless: after a successful sign-in the user's details live in
   an AES-encrypted session cookie (Ring's cookie store), so any dyno can serve
   any request without shared server state.

   The domain gate is deliberately belt-and-braces: we require Google's `hd`
   (hosted-domain) claim to match, the email to sit under that domain, and the
   email to be verified. Nothing else may sign in."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [ring.middleware.session.cookie :as cookie]
            [ring.util.response :as resp]
            [whitehouse.views :as views])
  (:import (java.net URI URLEncoder)
           (java.net.http HttpClient
                          HttpRequest
                          HttpRequest$BodyPublishers
                          HttpResponse$BodyHandlers)
           (java.nio.charset StandardCharsets)
           (java.security SecureRandom)
           (java.util Arrays Base64)))

;; ---------------------------------------------------------------------------
;; Configuration (all from the environment — never committed)
;; ---------------------------------------------------------------------------

(defn- env
  ([k] (System/getenv k))
  ([k default] (or (System/getenv k) default)))

(defn config
  "OAuth configuration, read fresh from the environment on each call."
  []
  {:client-id     (env "GOOGLE_CLIENT_ID")
   :client-secret (env "GOOGLE_CLIENT_SECRET")
   :redirect-uri  (env "OAUTH_REDIRECT_URI" "http://localhost:3000/oauth/callback")
   :domain        (env "ALLOWED_DOMAIN" "whitehouse.org.uk")
   :admins        (->> (str/split (env "ADMIN_EMAILS" "") #",")
                       (map (comp str/lower-case str/trim))
                       (remove str/blank?)
                       set)})

(defn configured?
  "True once the Google client credentials are present in the environment."
  []
  (let [{:keys [client-id client-secret]} (config)]
    (boolean (and (not (str/blank? client-id))
                  (not (str/blank? client-secret))))))

(def ^:private auth-endpoint  "https://accounts.google.com/o/oauth2/v2/auth")
(def ^:private token-endpoint "https://oauth2.googleapis.com/token")

;; ---------------------------------------------------------------------------
;; Small helpers
;; ---------------------------------------------------------------------------

(defn- enc [s]
  (URLEncoder/encode (str s) StandardCharsets/UTF_8))

(defn- query-string [params]
  (str/join "&" (for [[k v] params :when (some? v)]
                  (str (name k) "=" (enc v)))))

(defn- random-token
  "A URL-safe, unguessable token for CSRF `state`."
  []
  (let [b (byte-array 32)]
    (.nextBytes (SecureRandom.) b)
    (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) b)))

(defn- http-post-form
  "POST a form-encoded body and return {:status :body}. Uses the JDK client so
   we add no HTTP dependency."
  [url params]
  (let [client (HttpClient/newHttpClient)
        req    (-> (HttpRequest/newBuilder (URI/create url))
                   (.header "Content-Type" "application/x-www-form-urlencoded")
                   (.header "Accept" "application/json")
                   (.POST (HttpRequest$BodyPublishers/ofString (query-string params)))
                   (.build))
        res    (.send client req (HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode res) :body (.body res)}))

;; ---------------------------------------------------------------------------
;; The OAuth dance
;; ---------------------------------------------------------------------------

(defn authorize-url
  "Where we send the browser to begin sign-in. `hd` asks Google to pre-filter to
   the family domain; we still verify server-side on return."
  [state]
  (let [{:keys [client-id redirect-uri domain]} (config)]
    (str auth-endpoint "?"
         (query-string {:client_id     client-id
                        :redirect_uri  redirect-uri
                        :response_type "code"
                        :scope         "openid email profile"
                        :state         state
                        :hd            domain
                        :access_type   "online"
                        :prompt        "select_account"}))))

(defn- exchange-code
  "Swap an authorization code for tokens. Returns the parsed body or nil."
  [code]
  (let [{:keys [client-id client-secret redirect-uri]} (config)
        {:keys [status body]}
        (http-post-form token-endpoint
                        {:code          code
                         :client_id     client-id
                         :client_secret client-secret
                         :redirect_uri  redirect-uri
                         :grant_type    "authorization_code"})]
    (when (= 200 status)
      (json/read-str body :key-fn keyword))))

(defn- decode-jwt-claims
  "Read the claims out of an id_token. The token comes straight from Google's
   token endpoint over TLS (server-to-server), so per Google's guidance we can
   trust the payload without re-verifying the signature."
  [id-token]
  (let [payload (second (str/split id-token #"\."))
        decoded (String. (.decode (Base64/getUrlDecoder) payload)
                         StandardCharsets/UTF_8)]
    (json/read-str decoded :key-fn keyword)))

(defn- authorised-user
  "Turn verified id_token claims into a session user, or nil if the account is
   not an authorised family member."
  [{:keys [email email_verified hd name picture]}]
  (let [{:keys [domain admins]} (config)
        email* (some-> email str/lower-case)]
    (when (and email*
               email_verified
               (= hd domain)
               (str/ends-with? email* (str "@" domain)))
      {:email   email
       :name    (or name email)
       :picture picture
       :admin   (contains? admins email*)})))

;; ---------------------------------------------------------------------------
;; Ring handlers
;; ---------------------------------------------------------------------------

(defn- html [body]
  (-> (resp/response body)
      (resp/content-type "text/html; charset=utf-8")))

(defn- login-page
  ([] (login-page nil))
  ([error] (html (views/login {:configured (configured?) :error error}))))

(defn login
  "GET /login — the sign-in page."
  [_]
  (login-page))

(defn start
  "GET /oauth/start — mint a state token and redirect to Google."
  [request]
  (if (configured?)
    (let [state (random-token)]
      (-> (resp/redirect (authorize-url state))
          (assoc :session (assoc (:session request) ::state state))))
    (login-page "Sign-in isn't configured yet. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.")))

(defn callback
  "GET /oauth/callback — verify state, exchange the code, gate the domain."
  [request]
  (let [session  (:session request)
        expected (::state session)
        code     (get-in request [:query-params "code"])
        state    (get-in request [:query-params "state"])
        error    (get-in request [:query-params "error"])
        clear    #(assoc % :session (dissoc session ::state))]
    (cond
      error
      (clear (login-page "Sign-in was cancelled."))

      (or (str/blank? state) (not= state expected))
      (clear (login-page "Sign-in couldn't be verified. Please try again."))

      :else
      (if-let [tokens (exchange-code code)]
        (if-let [user (authorised-user (decode-jwt-claims (:id_token tokens)))]
          (-> (resp/redirect "/")
              (assoc :session (-> session (dissoc ::state) (assoc :user user))))
          (clear (login-page "That account isn't a whitehouse.org.uk family account.")))
        (clear (login-page "Couldn't complete sign-in with Google. Please try again."))))))

(defn logout
  "GET /logout — drop the session cookie."
  [_]
  (assoc (resp/redirect "/login") :session nil))

;; ---------------------------------------------------------------------------
;; Middleware & session store
;; ---------------------------------------------------------------------------

(defn current-user [request]
  (get-in request [:session :user]))

(def ^:private public-prefixes
  ["/login" "/oauth" "/logout" "/css/" "/favicon"])

(defn- public? [uri]
  (boolean (some #(str/starts-with? uri %) public-prefixes)))

(defn wrap-require-auth
  "Gate every route except the login flow and static assets. Unauthenticated
   requests are bounced to /login."
  [handler]
  (fn [request]
    (if (or (public? (:uri request))
            (current-user request))
      (handler request)
      (resp/redirect "/login"))))

(defn- session-key
  "A 16-byte AES-128 key derived from SESSION_SECRET, or nil to let the cookie
   store generate an ephemeral one (fine for local dev)."
  []
  (when-let [s (env "SESSION_SECRET")]
    (Arrays/copyOf (.getBytes s StandardCharsets/UTF_8) 16)))

(defn session-store []
  (if-let [k (session-key)]
    (cookie/cookie-store {:key k})
    (cookie/cookie-store)))

(defn session-options []
  {:store        (session-store)
   :cookie-name  "wh-session"
   :cookie-attrs {:http-only true
                  :same-site :lax
                  :secure    (str/starts-with? (:redirect-uri (config)) "https")}})
