package soa.blasa.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Configuration de l'authentification, lue dans les variables d'environnement :
 *   AUTH_ENABLED     "true" pour exiger un jeton sur les ecritures (defaut : false, API ouverte comme avant)
 *   JWT_SECRET       secret de signature (>= 16 caracteres) ; sinon un secret aleatoire est genere a chaque demarrage
 *   JWT_TTL_SECONDS  duree de vie d'un jeton (defaut : 3600)
 */
public final class AuthConfig {

    private static final boolean ENABLED = "true".equalsIgnoreCase(System.getenv("AUTH_ENABLED"));
    private static final Jwt JWT = create();

    private AuthConfig() { }

    public static boolean enabled() {
        return ENABLED;
    }

    public static Jwt jwt() {
        return JWT;
    }

    private static Jwt create() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            secret = Base64.getEncoder().encodeToString(random);
            if (ENABLED) {
                System.out.println("JWT_SECRET absent : secret aleatoire genere (les jetons ne survivent pas a un redemarrage).");
            }
        }
        long ttl = Long.parseLong(System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600"));
        return new Jwt(secret.getBytes(StandardCharsets.UTF_8), ttl);
    }
}
