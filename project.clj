(defproject io.github.notusinggithub/wan27-clj "0.1.0"
  :description "Clojure client SDK for Wan2.7 AI image and video generation API, cost estimation, task polling, and MCP Server workflows (https://wan2-7.io/)."
  :url "https://wan2-7.io/"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "https://github.com/NotUsingGithub/wan27-clj"
        :tag "v0.1.0"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :sign-releases false}]])
