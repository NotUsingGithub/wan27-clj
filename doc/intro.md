# Getting Started with Wan2.7 AI for Clojure (`wan27-clj`)

`io.github.notusinggithub/wan27-clj` provides an idiomatic, zero-dependency Clojure SDK for the **[Wan2.7 AI](https://wan2-7.io/)** image and video generation platform.

## Official Wan2.7 Documentation & Resources

- **Wan2.7 AI Official Website**: [https://wan2-7.io/](https://wan2-7.io/)
- **Wan2.7 MCP Server Overview**: [https://wan2-7.io/mcp/](https://wan2-7.io/mcp/)
- **Wan2.7 MCP Setup Guide**: [https://wan2-7.io/mcp/guide/](https://wan2-7.io/mcp/guide/)
- **Wan2.7 CLI Reference**: [https://wan2-7.io/cli/](https://wan2-7.io/cli/)
- **Wan2.7 Pricing & Credits**: [https://wan2-7.io/pricing/](https://wan2-7.io/pricing/)

## Installation

### Clojure CLI (`deps.edn`)

```clojure
{:deps {io.github.notusinggithub/wan27-clj {:mvn/version "0.1.0"}}}
```

### Leiningen (`project.clj`)

```clojure
[io.github.notusinggithub/wan27-clj "0.1.0"]
```

## Authentication & Billing Safety

Wan2.7 generation endpoints require an active account and credits (see [Wan2.7 Pricing](https://wan2-7.io/pricing/)). Set your token in `WAN27_ACCESS_TOKEN` or pass `:access-token` explicitly to `wan27/client`:

```clojure
(require '[wan27 :as wan27])

(def c (wan27/client {:access-token (System/getenv "WAN27_ACCESS_TOKEN")}))

;; 1. Check account credit balance
(wan27/account-balance c)

;; 2. Estimate cost before running a paid generation
(wan27/estimate-cost c {:model "wan2.7-image"
                        :prompt "A serene mountain lake at dawn"
                        :aspect-ratio "16:9"})

;; 3. Submit an idempotent generation task with a unique 16-128 char request-id
(wan27/generate-image c {:prompt "A serene mountain lake at dawn"
                         :request-id "req_20261028_lake_0001"
                         :aspect-ratio "16:9"})
```

## Using Wan2.7 with MCP Clients (Claude Desktop, Cursor, Windsurf)

Alongside this Clojure SDK, you can connect AI coding assistants directly to Wan2.7 via the official Model Context Protocol server (`@wan27/cli`). Read the full setup walkthrough at [https://wan2-7.io/mcp/guide/](https://wan2-7.io/mcp/guide/) and CLI docs at [https://wan2-7.io/cli/](https://wan2-7.io/cli/).
