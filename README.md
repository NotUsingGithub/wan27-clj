# wan27-clj — Clojure Client SDK for Wan2.7 AI

[![Clojars Project](https://img.shields.io/clojars/v/io.github.notusinggithub/wan27-clj.svg)](https://clojars.org/io.github.notusinggithub/wan27-clj)
[![cljdoc badge](https://cljdoc.org/badge/io.github.notusinggithub/wan27-clj)](https://cljdoc.org/d/io.github.notusinggithub/wan27-clj)

`io.github.notusinggithub/wan27-clj` is a lightweight, zero-dependency Clojure client library for **[Wan2.7 AI](https://wan2-7.io/)** image and video generation, credit balance inspection, cost estimation, and task polling.

## Official Links

- **Official Website**: [https://wan2-7.io/](https://wan2-7.io/)
- **MCP Server Overview**: [https://wan2-7.io/mcp/](https://wan2-7.io/mcp/)
- **MCP Setup Guide**: [https://wan2-7.io/mcp/guide/](https://wan2-7.io/mcp/guide/)
- **CLI Documentation**: [https://wan2-7.io/cli/](https://wan2-7.io/cli/)
- **Pricing & Credits**: [https://wan2-7.io/pricing/](https://wan2-7.io/pricing/)
- **Source Repository**: [https://github.com/NotUsingGithub/wan27-clj](https://github.com/NotUsingGithub/wan27-clj)

## Installation

### `deps.edn`

```clojure
io.github.notusinggithub/wan27-clj {:mvn/version "0.1.0"}
```

### Leiningen `project.clj`

```clojure
[io.github.notusinggithub/wan27-clj "0.1.0"]
```

## Quick Start

```clojure
(ns my-app.core
  (:require [wan27 :as wan27]))

;; Initialize client (reads WAN27_ACCESS_TOKEN by default)
(def client (wan27/client))

;; Inspect canonical platform URLs
wan27/urls
;; => {:website "https://wan2-7.io/", :mcp "https://wan2-7.io/mcp/", ...}

;; Check credit balance (see https://wan2-7.io/pricing/)
(wan27/account-balance client)

;; Estimate generation credits first
(wan27/estimate-cost client {:model "wan2.7-image"
                             :prompt "Architectural sketch of a modern glass pavilion"
                             :aspect-ratio "16:9"})

;; Submit idempotent image generation with a 16-128 character request-id
(wan27/generate-image client {:prompt "Architectural sketch of a modern glass pavilion"
                              :request-id "req_20261028_pavilion_0001"
                              :aspect-ratio "16:9"})

;; Poll task status
(wan27/get-task client "task_01JXYZ")
```

## License

MIT License — see [LICENSE](LICENSE).
