package com.elecciones.security;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class HmacService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "HMAC-SHA256:";

    private final String hmacSecret;
    private final ObjectMapper canonicalObjectMapper;

    public HmacService(@Value("${HMAC_SECRET:${hmac.secret:ChangeThisHmacSecret_Min32Chars_Dev_2026}}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
        this.canonicalObjectMapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String signPayload(Map<String, Object> payload) {
        try {
            String canonicalJson = canonicalObjectMapper.writeValueAsString(payload);

            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec key = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );

            mac.init(key);

            byte[] digest = mac.doFinal(canonicalJson.getBytes(StandardCharsets.UTF_8));

            return SIGNATURE_PREFIX + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el payload de auditoría", ex);
        }
    }

    public boolean verifyPayload(Map<String, Object> payload, String receivedSignature) {
        if (receivedSignature == null || !receivedSignature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expectedSignature = signPayload(payload);

        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}