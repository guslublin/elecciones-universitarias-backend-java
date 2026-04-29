package com.elecciones.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
public class HashService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${VOTER_HASH_SECRET:${JWT_SECRET:local-development-voter-hash-secret-1234567890}}")
    private String voterHashSecret;

    public String hashVoter(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec key = new SecretKeySpec(
                    voterHashSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );

            mac.init(key);

            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el hash anónimo del votante", ex);
        }
    }
}