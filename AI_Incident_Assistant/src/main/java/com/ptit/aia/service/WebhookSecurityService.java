package com.ptit.aia.service;

import com.ptit.aia.config.AiaProperties;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class WebhookSecurityService {
    private final AiaProperties properties;

    public WebhookSecurityService(AiaProperties properties) {
        this.properties = properties;
    }

    public boolean validateTelegram(String secretToken) {
        return properties.getTelegram().getBotSecretToken() == null
                || properties.getTelegram().getBotSecretToken().equals(secretToken);
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot calculate HMAC-SHA256", ex);
        }
    }
}
