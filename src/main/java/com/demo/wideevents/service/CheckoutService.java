package com.demo.wideevents.service;

import com.demo.wideevents.domain.WideEvent;
import com.demo.wideevents.domain.WideEventContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class CheckoutService {

    private static final String[] SUBSCRIPTIONS = {"free", "basic", "premium", "enterprise"};
    private static final String[] PAYMENT_METHODS = {"credit_card", "debit_card", "pix", "boleto"};
    private static final String[] PROVIDERS = {"stripe", "pagseguro", "mercadopago", "cielo"};
    private static final String[] COUPONS = {null, null, null, "SAVE10", "FIRST20", "BLACK50", "WELCOME15"};
    private static final String[] ERROR_CODES = {"card_declined", "insufficient_funds", "expired_card", "provider_timeout", "fraud_suspected"};

    private final Random random = ThreadLocalRandom.current();

    public Map<String, Object> processCheckout(String userId, WideEvent.WideEventBuilder wideEvent) {
        var subscription = randomFrom(SUBSCRIPTIONS);
        var accountAgeDays = random.nextInt(1, 1500);
        var ltv = random.nextLong(0, 500_000);

        wideEvent.userId(userId != null ? userId : "user_" + random.nextInt(1000, 9999))
               .userSubscription(subscription)
               .userAccountAgeDays(accountAgeDays)
               .userLifetimeValueCents(ltv)
               .action("checkout");

        var cartId = "cart_" + random.nextInt(10000, 99999);
        var itemCount = random.nextInt(1, 8);
        var totalCents = random.nextLong(999, 150_000);
        var coupon = randomFrom(COUPONS);

        wideEvent.cartId(cartId)
               .cartItemCount(itemCount)
               .cartTotalCents(totalCents);

        if (coupon != null) {
            var discount = Integer.parseInt(coupon.replaceAll("[^0-9]", ""));
            wideEvent.couponCode(coupon)
                   .couponDiscountPercent(discount);
        }

        wideEvent.featureFlags(Map.of(
                "new_checkout_flow", random.nextBoolean(),
                "express_payment", random.nextDouble() < 0.3,
                "smart_retry", random.nextDouble() < 0.5
        ));

        var paymentMethod = randomFrom(PAYMENT_METHODS);
        var provider = randomFrom(PROVIDERS);
        var attempt = random.nextDouble() < 0.2 ? random.nextInt(2, 4) : 1;

        var baseLatency = switch (provider) {
            case "stripe" -> 150;
            case "pagseguro" -> 300;
            case "mercadopago" -> 250;
            case "cielo" -> 200;
            default -> 200;
        };
        var latencyMs = baseLatency + random.nextInt(-50, 500);

        wideEvent.paymentMethod(paymentMethod)
               .paymentProvider(provider)
               .paymentLatencyMs((long) Math.max(50, latencyMs))
               .paymentAttempt(attempt);

        sleep(Math.max(50, latencyMs));

        var errorRate = calculateErrorRate(subscription, provider, attempt, totalCents);
        var failed = random.nextDouble() < errorRate;

        if (failed) {
            var errorCode = randomFrom(ERROR_CODES);
            wideEvent.outcome("error")
                   .errorType("PaymentException")
                   .errorCode(errorCode)
                   .errorMessage("Payment failed: " + errorCode)
                   .errorRetriable(!"fraud_suspected".equals(errorCode));

            return Map.of(
                    "success", false,
                    "error", errorCode,
                    "cart_id", cartId
            );
        }

        wideEvent.outcome("success");

        return Map.of(
                "success", true,
                "order_id", "order_" + random.nextInt(100000, 999999),
                "cart_id", cartId,
                "total_cents", totalCents
        );
    }

    private double calculateErrorRate(String subscription, String provider, int attempt, long totalCents) {
        double rate = 0.08; // base 8%

        if ("cielo".equals(provider)) rate += 0.07;
        if ("mercadopago".equals(provider)) rate += 0.04;

        if (attempt > 1) rate += 0.15;

        if (totalCents > 100_000) rate += 0.10;

        if ("premium".equals(subscription) || "enterprise".equals(subscription)) rate -= 0.03;

        return Math.min(rate, 0.5);
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(Math.min(ms, 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T randomFrom(T[] array) {
        return array[random.nextInt(array.length)];
    }
}
