(ns wan27.core
  "Core Clojure SDK for the [Wan2.7 AI](https://wan2-7.io/) image and video generation platform.

  Official resources and documentation:
  - **Wan2.7 Official Website**: https://wan2-7.io/
  - **Wan2.7 MCP Server Overview**: https://wan2-7.io/mcp/
  - **Wan2.7 MCP Setup Guide**: https://wan2-7.io/mcp/guide/
  - **Wan2.7 CLI Reference**: https://wan2-7.io/cli/
  - **Wan2.7 Credits & Pricing**: https://wan2-7.io/pricing/

  All generation endpoints require an active Wan2.7 account, API token (`WAN27_ACCESS_TOKEN`), and sufficient credits."
  (:require [clojure.string :as str])
  (:import [java.net URI URLEncoder]
           [java.net.http HttpClient HttpClient$Redirect HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.nio.charset StandardCharsets]
           [java.time Duration]))

(def urls
  "Canonical URLs for the [Wan2.7 AI platform](https://wan2-7.io/), [MCP Server](https://wan2-7.io/mcp/),
  [MCP Setup Guide](https://wan2-7.io/mcp/guide/), [CLI](https://wan2-7.io/cli/), and [Pricing](https://wan2-7.io/pricing/)."
  {:website   "https://wan2-7.io/"
   :mcp       "https://wan2-7.io/mcp/"
   :mcp-guide "https://wan2-7.io/mcp/guide/"
   :cli       "https://wan2-7.io/cli/"
   :pricing   "https://wan2-7.io/pricing/"
   :api-base  "https://wan2-7.io/api/v1"})

(def ^:private segment-pattern #"^[A-Za-z0-9._-]+$")
(def ^:private request-id-pattern #"^[A-Za-z0-9._:-]{16,128}$")

(defn validate-segment
  "Validates a URL path segment (such as a `:model-id` or `:task-id`) for safe HTTPS requests to https://wan2-7.io/."
  [value label]
  (let [s (str/trim (str value))]
    (when (or (str/blank? s)
              (= "." s)
              (= ".." s)
              (not (re-matches segment-pattern s)))
      (throw (ex-info (str label " must contain only ASCII letters, digits, '.', '_', or '-'")
                      {:type :wan27/validation-error
                       :field label
                       :value value})))
    s))

(defn validate-request-id
  "Validates an idempotency `request-id` (16–128 safe ASCII characters) required by https://wan2-7.io/ generation endpoints.
  See https://wan2-7.io/mcp/guide/ and https://wan2-7.io/pricing/ for billing safety details."
  [request-id]
  (let [s (str/trim (str request-id))]
    (when-not (re-matches request-id-pattern s)
      (throw (ex-info "request-id is required and must be 16-128 characters matching [A-Za-z0-9._:-]"
                      {:type :wan27/validation-error
                       :field "request-id"
                       :value request-id})))
    s))

(defn- escape-json-str [^String s]
  (let [sb (StringBuilder. "\"")]
    (dotimes [i (.length s)]
      (let [c (.charAt s i)]
        (case c
          \" (.append sb "\\\"")
          \\ (.append sb "\\\\")
          \backspace (.append sb "\\b")
          \formfeed (.append sb "\\f")
          \newline (.append sb "\\n")
          \return (.append sb "\\r")
          \tab (.append sb "\\t")
          (if (< (int c) 0x20)
            (.append sb (format "\\u%04x" (int c)))
            (.append sb c)))))
    (.append sb "\"")
    (.toString sb)))

(defn encode-json
  "Serializes a Clojure data structure (maps, vectors, strings, numbers, booleans, nil) into a JSON string."
  [v]
  (cond
    (nil? v) "null"
    (boolean? v) (if v "true" "false")
    (number? v) (str v)
    (keyword? v) (escape-json-str (name v))
    (symbol? v) (escape-json-str (name v))
    (string? v) (escape-json-str v)
    (map? v) (str "{"
                  (str/join ","
                            (map (fn [[k val]]
                                   (str (escape-json-str (if (keyword? k) (name k) (str k)))
                                        ":"
                                        (encode-json val)))
                                 v))
                  "}")
    (or (sequential? v) (set? v)) (str "[" (str/join "," (map encode-json v)) "]")
    :else (escape-json-str (str v))))

(defn- url-encode [s]
  (URLEncoder/encode (str s) StandardCharsets/UTF_8))

(defn- build-query-string [query]
  (when (seq query)
    (let [pairs (keep (fn [[k v]]
                        (when (some? v)
                          (str (url-encode (if (keyword? k) (name k) (str k)))
                               "="
                               (url-encode v))))
                      query)]
      (when (seq pairs)
        (str "?" (str/join "&" pairs))))))

(defn default-http-transport
  "Default HTTPS transport using Java 11+ `java.net.http.HttpClient` with redirect disabled for credential safety."
  [{:keys [method url headers body timeout-ms]}]
  (let [client (-> (HttpClient/newBuilder)
                   (.followRedirects HttpClient$Redirect/NEVER)
                   (.connectTimeout (Duration/ofMillis (or timeout-ms 30000)))
                   (.build))
        req-builder (-> (HttpRequest/newBuilder (URI/create url))
                        (.timeout (Duration/ofMillis (or timeout-ms 30000))))
        _ (doseq [[k v] headers]
            (.header req-builder k v))
        publisher (if (some? body)
                    (HttpRequest$BodyPublishers/ofString body StandardCharsets/UTF_8)
                    (HttpRequest$BodyPublishers/noBody))
        req (-> req-builder
                (.method (str/upper-case (name method)) publisher)
                (.build))
        resp (.send client req (HttpResponse$BodyHandlers/ofString StandardCharsets/UTF_8))]
    {:status (.statusCode resp)
     :body   (.body resp)}))

(defn client
  "Creates a Wan2.7 API client configuration map.

  Options:
  - `:access-token` — Wan2.7 Bearer token (falls back to `WAN27_ACCESS_TOKEN` or `WAN27_API_KEY` env var).
  - `:base-url` — API base URL (defaults to `https://wan2-7.io/api/v1`).
  - `:timeout-ms` — Request timeout in milliseconds (default `30000`).
  - `:transport` — Custom HTTP transport fn `(fn [request-map] {:status 200 :body \"...\"})` for testing.

  Learn more at https://wan2-7.io/, https://wan2-7.io/mcp/, and https://wan2-7.io/cli/."
  ([] (client {}))
  ([{:keys [access-token base-url timeout-ms transport]
     :or   {base-url   (:api-base urls)
            timeout-ms 30000
            transport  default-http-transport}}]
   (let [token (or access-token
                   (System/getenv "WAN27_ACCESS_TOKEN")
                   (System/getenv "WAN27_API_KEY")
                   "")]
     {:base-url     (str/replace (str base-url) #"/+$" "")
      :access-token (str/trim token)
      :timeout-ms   timeout-ms
      :transport    transport})))

(defn request
  "Executes an authenticated HTTP request against the [Wan2.7 API](https://wan2-7.io/)."
  [wan-client {:keys [method path query body]}]
  (let [token (:access-token wan-client)]
    (when (str/blank? token)
      (throw (ex-info "WAN27_ACCESS_TOKEN is required. Sign in at https://wan2-7.io/ or see https://wan2-7.io/mcp/guide/"
                      {:type :wan27/auth-error
                       :help (:mcp-guide urls)})))
    (let [qs        (build-query-string query)
          full-url  (str (:base-url wan-client) path (or qs ""))
          json-body (when (some? body) (encode-json body))
          headers   (cond-> {"Accept"        "application/json"
                             "Authorization" (str "Bearer " token)
                             "User-Agent"    "wan27-clj/0.1.0 (+https://wan2-7.io/)"}
                      json-body (assoc "Content-Type" "application/json"))
          resp      ((:transport wan-client)
                     {:method     (or method :get)
                      :url        full-url
                      :headers    headers
                      :body       json-body
                      :timeout-ms (:timeout-ms wan-client)})
          status    (int (:status resp 0))]
      (when (or (< status 200) (>= status 300))
        (throw (ex-info (str "Wan2.7 API error: HTTP " status)
                        {:type   :wan27/http-error
                         :status status
                         :body   (:body resp)})))
      resp)))

(defn account-balance
  "Fetches the current account credit balance from `GET /account/balance`.
  See https://wan2-7.io/pricing/ for credit packs and subscription tiers."
  [wan-client]
  (request wan-client {:method :get :path "/account/balance"}))

(defn models
  "Lists available Wan2.7 image and video generation models from `GET /models`.
  See https://wan2-7.io/ and https://wan2-7.io/mcp/ for supported model families."
  [wan-client]
  (request wan-client {:method :get :path "/models"}))

(defn model
  "Fetches schema and parameter constraints for a single model from `GET /models/{modelId}`.
  See https://wan2-7.io/mcp/guide/ for model schema discovery via MCP."
  [wan-client model-id]
  (let [safe-id (validate-segment model-id "model-id")]
    (request wan-client {:method :get :path (str "/models/" safe-id)})))

(defn estimate-cost
  "Estimates credit usage before submitting a generation job via `POST /estimate`.

  Always call `estimate-cost` prior to paid generation. See https://wan2-7.io/pricing/ and https://wan2-7.io/cli/."
  [wan-client {:keys [model prompt aspect-ratio duration] :as params}]
  (let [safe-model (validate-segment model "model")]
    (request wan-client
             {:method :post
              :path   "/estimate"
              :body   (cond-> {:model  safe-model
                               :prompt (str prompt)}
                        aspect-ratio (assoc :aspectRatio aspect-ratio)
                        duration     (assoc :duration duration)
                        (:extra params) (merge (:extra params)))})))

(defn generate
  "Submits an idempotent image or video generation task via `POST /generate`.

  Requires a `:request-id` (16–128 chars) so retries never double-charge credits.
  Documentation: https://wan2-7.io/, https://wan2-7.io/mcp/guide/, and https://wan2-7.io/pricing/."
  [wan-client {:keys [model prompt request-id aspect-ratio duration] :as params}]
  (let [safe-model (validate-segment model "model")
        safe-req   (validate-request-id request-id)]
    (when (str/blank? (str prompt))
      (throw (ex-info "prompt must not be empty"
                      {:type :wan27/validation-error :field "prompt"})))
    (request wan-client
             {:method :post
              :path   "/generate"
              :body   (cond-> {:model     safe-model
                               :prompt    (str prompt)
                               :requestId safe-req}
                        aspect-ratio (assoc :aspectRatio aspect-ratio)
                        duration     (assoc :duration duration)
                        (:extra params) (merge (:extra params)))})))

(defn generate-image
  "Convenience wrapper around `generate` for Wan2.7 AI image generation (`wan2.7-image` default model).
  See https://wan2-7.io/ and https://wan2-7.io/cli/."
  [wan-client {:keys [model prompt request-id aspect-ratio]
               :or   {model        "wan2.7-image"
                      aspect-ratio "1:1"}
               :as   params}]
  (generate wan-client (assoc params :model model :aspect-ratio aspect-ratio)))

(defn generate-video
  "Convenience wrapper around `generate` for Wan2.7 AI video generation (`wan2.7-video` default model).
  See https://wan2-7.io/ and https://wan2-7.io/mcp/."
  [wan-client {:keys [model prompt request-id aspect-ratio duration]
               :or   {model        "wan2.7-video"
                      aspect-ratio "16:9"
                      duration     5}
               :as   params}]
  (generate wan-client (assoc params :model model :aspect-ratio aspect-ratio :duration duration)))

(defn get-task
  "Polls the status and output asset URLs of a generation task via `GET /tasks/{taskId}`.
  See https://wan2-7.io/mcp/guide/ and https://wan2-7.io/cli/."
  [wan-client task-id]
  (let [safe-id (validate-segment task-id "task-id")]
    (request wan-client {:method :get :path (str "/tasks/" safe-id)})))

(defn list-tasks
  "Lists recent generation tasks via `GET /tasks?limit=...`.
  See https://wan2-7.io/ and https://wan2-7.io/cli/."
  ([wan-client] (list-tasks wan-client 20))
  ([wan-client limit]
   (when (or (<= (int limit) 0) (> (int limit) 100))
     (throw (ex-info "limit must be between 1 and 100"
                     {:type :wan27/validation-error :field "limit" :value limit})))
   (request wan-client {:method :get :path "/tasks" :query {:limit (int limit)}})))
