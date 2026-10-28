(ns wan27.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [wan27 :as wan27]
            [wan27.core :as core]))

(deftest urls-test
  (testing "Canonical Wan2.7 URLs are exposed on both wan27 and wan27.core"
    (is (= "https://wan2-7.io/" (:website core/urls)))
    (is (= "https://wan2-7.io/mcp/" (:mcp core/urls)))
    (is (= "https://wan2-7.io/mcp/guide/" (:mcp-guide core/urls)))
    (is (= "https://wan2-7.io/cli/" (:cli core/urls)))
    (is (= "https://wan2-7.io/pricing/" (:pricing core/urls)))
    (is (= core/urls wan27/urls))))

(deftest validation-test
  (testing "Segment and request-id validation enforce safe characters"
    (is (= "wan2.7-image" (core/validate-segment "wan2.7-image" "model")))
    (is (thrown? clojure.lang.ExceptionInfo (core/validate-segment "../secret" "model")))
    (is (= "req_20261028_0001_abcd" (core/validate-request-id "req_20261028_0001_abcd")))
    (is (thrown? clojure.lang.ExceptionInfo (core/validate-request-id "short")))))

(deftest offline-mock-client-test
  (testing "Client builds expected HTTP requests and handles responses offline"
    (let [captured (atom nil)
          mock-transport (fn [req]
                           (reset! captured req)
                           {:status 200
                            :body   "{\"ok\":true,\"taskId\":\"task_12345\"}"})
          c (wan27/client {:access-token "test_token_123456"
                           :transport    mock-transport})
          resp (wan27/generate-image c {:prompt       "A minimalist ceramic tea cup"
                                        :request-id   "req_20261028_clojure_0001"
                                        :aspect-ratio "16:9"})]
      (is (= 200 (:status resp)))
      (is (= :post (:method @captured)))
      (is (= "https://wan2-7.io/api/v1/generate" (:url @captured)))
      (is (= "Bearer test_token_123456" (get-in @captured [:headers "Authorization"])))
      (is (str/includes? (:body @captured) "\"requestId\":\"req_20261028_clojure_0001\""))
      (is (str/includes? (:body @captured) "\"aspectRatio\":\"16:9\"")))))

(deftest missing-token-test
  (testing "Requests without WAN27_ACCESS_TOKEN fail fast before network call"
    (let [c (wan27/client {:access-token ""
                           :transport    (fn [_] {:status 200 :body "{}"})})]
      (is (thrown? clojure.lang.ExceptionInfo (wan27/account-balance c))))))
