package soa.blasa.repository;

import org.junit.jupiter.api.Assumptions;

import redis.clients.jedis.Jedis;

/**
 * Aide pour les tests qui ont besoin de Redis. Ils utilisent la base 15 (voir pom.xml :
 * -Dblasa.redis.db=15), jamais la base 0 des donnees de demo : un FLUSHDB ici est sans danger.
 * Si Redis n'est pas lance (docker compose up -d), ces tests sont ignores au lieu d'echouer.
 */
final class RedisTestSupport {

    private RedisTestSupport() { }

    /** Ignore le test si Redis est injoignable, et refuse de continuer si on n'est pas sur la base de test. */
    static void prepare() {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            redis.ping();
            if (RedisConfig.DB == 0) {
                throw new IllegalStateException("Les tests ne doivent pas tourner sur la base 0 (donnees de demo)");
            }
            redis.flushDB();
        } catch (redis.clients.jedis.exceptions.JedisConnectionException e) {
            Assumptions.abort("Redis injoignable (docker compose up -d) : tests ignores");
        }
    }
}
