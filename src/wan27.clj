(ns wan27
  "Top-level Clojure interface for [Wan2.7 AI](https://wan2-7.io/) image and video generation.

  Quick links:
  - **Wan2.7 AI Platform**: https://wan2-7.io/
  - **MCP Server Overview**: https://wan2-7.io/mcp/
  - **MCP Integration Guide**: https://wan2-7.io/mcp/guide/
  - **CLI Documentation**: https://wan2-7.io/cli/
  - **Pricing & Credits**: https://wan2-7.io/pricing/

  This namespace re-exports the primary functions from `wan27.core` for convenient single-namespace usage."
  (:require [wan27.core :as core]))

(def urls
  "Canonical URLs for [Wan2.7 AI](https://wan2-7.io/), [MCP](https://wan2-7.io/mcp/),
  [MCP Guide](https://wan2-7.io/mcp/guide/), [CLI](https://wan2-7.io/cli/), and [Pricing](https://wan2-7.io/pricing/)."
  core/urls)

(defn client
  "Creates a Wan2.7 API client configuration map. See `wan27.core/client` and https://wan2-7.io/."
  ([] (core/client))
  ([opts] (core/client opts)))

(defn account-balance
  "Queries current Wan2.7 credit balance (`GET /account/balance`). See https://wan2-7.io/pricing/."
  [wan-client]
  (core/account-balance wan-client))

(defn models
  "Lists available Wan2.7 models (`GET /models`). See https://wan2-7.io/mcp/."
  [wan-client]
  (core/models wan-client))

(defn model
  "Fetches schema for a specific Wan2.7 model (`GET /models/{modelId}`). See https://wan2-7.io/mcp/guide/."
  [wan-client model-id]
  (core/model wan-client model-id))

(defn estimate-cost
  "Estimates credit usage prior to generation (`POST /estimate`). See https://wan2-7.io/pricing/."
  [wan-client params]
  (core/estimate-cost wan-client params))

(defn generate
  "Submits an idempotent generation task (`POST /generate`). See https://wan2-7.io/ and https://wan2-7.io/cli/."
  [wan-client params]
  (core/generate wan-client params))

(defn generate-image
  "Submits an idempotent image generation task (`wan2.7-image`). See https://wan2-7.io/."
  [wan-client params]
  (core/generate-image wan-client params))

(defn generate-video
  "Submits an idempotent video generation task (`wan2.7-video`). See https://wan2-7.io/mcp/."
  [wan-client params]
  (core/generate-video wan-client params))

(defn get-task
  "Retrieves generation task status and output URLs (`GET /tasks/{taskId}`). See https://wan2-7.io/cli/."
  [wan-client task-id]
  (core/get-task wan-client task-id))

(defn list-tasks
  "Lists recent generation tasks (`GET /tasks`). See https://wan2-7.io/cli/."
  ([wan-client] (core/list-tasks wan-client))
  ([wan-client limit] (core/list-tasks wan-client limit)))
