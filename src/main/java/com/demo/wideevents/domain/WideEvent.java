package com.demo.wideevents.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WideEvent {

    // === Identificação da Request ===
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("trace_id")
    private String traceId;

    private String timestamp;

    // === Contexto do Serviço ===
    private String service;
    private String version;
    private String region;
    private String environment;

    // === HTTP ===
    @JsonProperty("http_method")
    private String httpMethod;

    @JsonProperty("http_path")
    private String httpPath;

    @JsonProperty("http_status")
    private Integer httpStatus;

    @JsonProperty("duration_ms")
    private Long durationMs;

    // === Quem (User Context) ===
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_subscription")
    private String userSubscription;

    @JsonProperty("user_account_age_days")
    private Integer userAccountAgeDays;

    @JsonProperty("user_lifetime_value_cents")
    private Long userLifetimeValueCents;

    // === O quê (Business Context) ===
    private String action;

    @JsonProperty("cart_id")
    private String cartId;

    @JsonProperty("cart_item_count")
    private Integer cartItemCount;

    @JsonProperty("cart_total_cents")
    private Long cartTotalCents;

    @JsonProperty("coupon_code")
    private String couponCode;

    @JsonProperty("coupon_discount_percent")
    private Integer couponDiscountPercent;

    // === Payment Context ===
    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("payment_provider")
    private String paymentProvider;

    @JsonProperty("payment_latency_ms")
    private Long paymentLatencyMs;

    @JsonProperty("payment_attempt")
    private Integer paymentAttempt;

    // === Resultado ===
    private String outcome;

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("error_retriable")
    private Boolean errorRetriable;

    // === Feature Flags ===
    @JsonProperty("feature_flags")
    private Map<String, Boolean> featureFlags;

    // === Evento de Negócio ===
    @JsonProperty("event_type")
    @Builder.Default
    private String eventType = "wide_event";
}
