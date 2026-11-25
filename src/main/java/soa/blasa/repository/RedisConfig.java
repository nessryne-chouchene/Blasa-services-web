package soa.blasa.repository;

import redis.clients.jedis.JedisPool;

/** Une seule connexion partagee (pool) vers Redis. */
public class RedisConfig {

    public static final String HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    private static final JedisPool POOL = new JedisPool(HOST, PORT);

    public static JedisPool pool() {
        return POOL;
    }
}
