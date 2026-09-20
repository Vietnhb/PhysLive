package com.example.backend.config.properties;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "physlive.vnpay")
public record VnpayProperties(
        String tmnCode,
        String hashSecret,
        String paymentUrl,
        String returnUrl,
        String queryUrl,
        String serverIp,
        ZoneId zoneId,
        Duration connectTimeout,
        Duration requestTimeout) {

    public VnpayProperties {
        tmnCode = clean(tmnCode);
        hashSecret = clean(hashSecret);
        paymentUrl = clean(paymentUrl);
        returnUrl = clean(returnUrl);
        queryUrl = clean(queryUrl);
        serverIp = clean(serverIp);
        if (zoneId == null) zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
        if (requestTimeout == null) requestTimeout = Duration.ofSeconds(20);
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("VNPay connect timeout must be positive");
        }
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("VNPay request timeout must be positive");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
