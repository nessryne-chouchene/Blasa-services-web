package soa.blasa.repository;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/** Une seule connexion partagee (pool) vers Redis. */
public class RedisConfig {

    public static final String HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    /**
     * Base Redis utilisee (0 par defaut = celle des donnees de demo).
     * Les tests passent -Dblasa.redis.db=15 pour ne jamais toucher aux donnees de demo.
     */
    public static final int DB = Integer.parseInt(
            System.getProperty("blasa.redis.db", System.getenv().getOrDefault("REDIS_DB", "0")));

    private static final JedisPool POOL = new JedisPool(new JedisPoolConfig(), HOST, PORT, Protocol.DEFAULT_TIMEOUT, null, DB);

    public static JedisPool pool() {
        return POOL;
    }
}
