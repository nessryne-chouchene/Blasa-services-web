package soa.blasa.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;
import soa.blasa.model.User;

/** Notes, classement des conducteurs (ZSET leaderboard:drivers) et compteurs (HASH stats:summary). */
public class StatsRepository {

    private static final String LEADERBOARD = "leaderboard:drivers";

    /** Ajoute une note (1 a 5). Retourne le profil mis a jour, ou null si l'utilisateur n'existe pas. */
    public User addRating(long userId, int score) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            String key = "user:" + userId;
            if (!redis.exists(key)) {
                return null;
            }
            long sum = redis.hincrBy(key, "ratingSum", score);
            long count = redis.hincrBy(key, "ratingCount", 1);
            redis.zadd(LEADERBOARD, (double) sum / count, String.valueOf(userId));   // moyenne = score du classement
            redis.hincrBy("stats:summary", "ratings", 1);
            Events.publish(redis, "rated", 0, "\"userId\":" + userId);
        }
        return new UserRepository().find(userId);
    }

    /** Les meilleurs conducteurs : ZREVRANGE (du plus grand score au plus petit). */
    public List<Map<String, Object>> topDrivers(int limit) {
        UserRepository users = new UserRepository();
        List<Map<String, Object>> out = new ArrayList<>();
        try (Jedis redis = RedisConfig.pool().getResource()) {
            for (Tuple t : redis.zrevrangeWithScores(LEADERBOARD, 0, limit - 1)) {
                User u = users.find(Long.parseLong(t.getElement()));
                if (u == null) {
                    redis.zrem(LEADERBOARD, t.getElement());         // utilisateur supprime entre-temps
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("userId", u.getId());
                row.put("name", u.getName());
                row.put("averageRating", Math.round(t.getScore() * 100.0) / 100.0);
                row.put("ratingCount", u.getRatingCount());
                out.add(row);
            }
        }
        return out;
    }

    /** Compteurs globaux : HGETALL stats:summary. */
    public Map<String, Long> summary() {
        Map<String, Long> out = new HashMap<>();
        out.put("ridesCreated", 0L);
        out.put("bookings", 0L);
        out.put("ratings", 0L);
        try (Jedis redis = RedisConfig.pool().getResource()) {
            for (Map.Entry<String, String> e : redis.hgetAll("stats:summary").entrySet()) {
                out.put(e.getKey(), Long.parseLong(e.getValue()));
            }
        }
        return out;
    }
}
