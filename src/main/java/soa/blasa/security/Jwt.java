package soa.blasa.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * JWT minimal : signature HS256 (HMAC-SHA256), JDK uniquement.
 * Le jeton contient l'id de l'utilisateur (sub), sa date d'emission (iat) et d'expiration (exp).
 * Dates en secondes Unix, comme le reste de l'API.
 */
public final class Jwt {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER = B64.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final Pattern SUB = Pattern.compile("\"sub\":\"(\\d{1,18})\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d{1,18})");

    private final byte[] secret;
    private final long ttlSeconds;

    public Jwt(byte[] secret, long ttlSeconds) {
        if (secret == null || secret.length < 16) {
            throw new IllegalArgumentException("le secret JWT doit faire au moins 16 caracteres");
        }
        this.secret = secret.clone();
        this.ttlSeconds = ttlSeconds;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    /** Cree un jeton pour cet utilisateur, valable ttlSeconds a partir de nowEpochSeconds. */
    public String issue(long userId, long nowEpochSeconds) {
        String payload = "{\"sub\":\"" + userId + "\",\"iat\":" + nowEpochSeconds
                + ",\"exp\":" + (nowEpochSeconds + ttlSeconds) + "}";
        String unsigned = HEADER + "." + B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return unsigned + "." + B64.encodeToString(hmac(unsigned));
    }

    /** L'id de l'utilisateur si le jeton est authentique et non expire, sinon null. */
    public Long verify(String token, long nowEpochSeconds) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !parts[0].equals(HEADER)) {   // refuse tout autre algorithme (ex. "none")
            return null;
        }
        try {
            byte[] given = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(hmac(parts[0] + "." + parts[1]), given)) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher sub = SUB.matcher(payload);
            Matcher exp = EXP.matcher(payload);
            if (!sub.find() || !exp.find() || Long.parseLong(exp.group(1)) <= nowEpochSeconds) {
                return null;
            }
            return Long.parseLong(sub.group(1));
        } catch (IllegalArgumentException e) {     // Base64 ou nombre invalide
            return null;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
