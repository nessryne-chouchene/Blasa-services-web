package soa.blasa.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class JwtTest {

    private static final long NOW = 1_000_000L;
    private final Jwt jwt = new Jwt("0123456789abcdef".getBytes(StandardCharsets.UTF_8), 60);

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void jetonValideRenvoieLUtilisateur() {
        assertEquals(7L, jwt.verify(jwt.issue(7, NOW), NOW + 10));
    }

    @Test
    void jetonExpire() {
        assertNull(jwt.verify(jwt.issue(7, NOW), NOW + 60));
    }

    @Test
    void signatureModifiee() {
        assertNull(jwt.verify(jwt.issue(7, NOW) + "x", NOW));
    }

    @Test
    void contenuFalsifieAvecLaSignatureOriginale() {
        String[] p = jwt.issue(7, NOW).split("\\.");
        String forged = p[0] + "." + b64("{\"sub\":\"1\",\"exp\":9999999999}") + "." + p[2];
        assertNull(jwt.verify(forged, NOW));
    }

    @Test
    void algorithmeNoneRefuse() {
        String[] p = jwt.issue(7, NOW).split("\\.");
        assertNull(jwt.verify(b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + p[1] + ".", NOW));
    }

    @Test
    void autreSecretRefuse() {
        Jwt other = new Jwt("another-secret-16ch".getBytes(StandardCharsets.UTF_8), 60);
        assertNull(other.verify(jwt.issue(7, NOW), NOW));
    }

    @Test
    void entreesInvalides() {
        assertNull(jwt.verify(null, NOW));
        assertNull(jwt.verify("n'importe quoi", NOW));
        assertNull(jwt.verify("a.b.c", NOW));
    }

    @Test
    void secretTropCourt() {
        assertThrows(IllegalArgumentException.class, () -> new Jwt("court".getBytes(StandardCharsets.UTF_8), 60));
    }
}
