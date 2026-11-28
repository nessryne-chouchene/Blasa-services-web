package soa.blasa.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import soa.blasa.model.User;
import soa.blasa.security.PasswordHasher;

/** Acces a Redis pour les utilisateurs : cle user:{id} de type HASH. */
public class UserRepository {

    private static String key(long id) {
        return "user:" + id;
    }

    /** POST : nouvel id grace au compteur atomique INCR, puis HSET. */
    public User create(User u) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            long id = redis.incr("seq:user");
            Map<String, String> fields = new HashMap<>();
            fields.put("name", u.getName());
            fields.put("email", u.getEmail());
            fields.put("city", u.getCity().toLowerCase());
            fields.put("ratingSum", "0");
            fields.put("ratingCount", "0");
            if (u.getRole() != null) {
                fields.put("role", u.getRole());
            }
            if (u.getPassword() != null && !u.getPassword().isBlank()) {
                fields.put("pwHash", PasswordHasher.hash(u.getPassword()));
            }
            redis.hset(key(id), fields);
            return find(id);
        }
    }

    /** Definit (ou change) le mot de passe : seul le hash est stocke. false si l'utilisateur n'existe pas. */
    public boolean setPassword(long id, String password) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            if (!redis.exists(key(id))) {
                return false;
            }
            redis.hset(key(id), "pwHash", PasswordHasher.hash(password));
            return true;
        }
    }

    /** Hash du mot de passe, ou null si l'utilisateur n'en a pas (comptes de demo). */
    public String passwordHash(long id) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            return redis.hget(key(id), "pwHash");
        }
    }

    /** GET : HGETALL ; un HASH vide veut dire que la cle n'existe pas. */
    public User find(long id) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            Map<String, String> h = redis.hgetAll(key(id));
            if (h.isEmpty()) {
                return null;
            }
            User u = new User();
            u.setId(id);
            u.setName(h.get("name"));
            u.setEmail(h.get("email"));
            u.setCity(h.get("city"));
            u.setRole(h.get("role"));
            u.setRatingSum(Integer.parseInt(h.getOrDefault("ratingSum", "0")));
            u.setRatingCount(Integer.parseInt(h.getOrDefault("ratingCount", "0")));
            return u;
        }
    }

    /** GET de la collection : SCAN des cles user:{id}, tries par id. */
    public List<User> findAll() {
        List<Long> ids = new ArrayList<>();
        try (Jedis redis = RedisConfig.pool().getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            do {
                ScanResult<String> page = redis.scan(cursor, new ScanParams().match("user:*").count(100));
                for (String k : page.getResult()) {
                    String id = k.substring("user:".length());
                    if (id.matches("\\d+")) {
                        ids.add(Long.parseLong(id));
                    }
                }
                cursor = page.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
        }
        Collections.sort(ids);
        List<User> out = new ArrayList<>();
        for (long id : ids) {
            User u = find(id);
            if (u != null) {
                out.add(u);
            }
        }
        return out;
    }

    /** PUT et PATCH : HSET sur les champs fournis seulement. */
    public User update(long id, Map<String, String> changes) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            if (!redis.exists(key(id))) {
                return null;
            }
            Map<String, String> fields = new HashMap<>(changes);
            if (fields.containsKey("city")) {
                fields.put("city", fields.get("city").toLowerCase());
            }
            redis.hset(key(id), fields);
            return find(id);
        }
    }

    /** DELETE : DEL renvoie le nombre de cles supprimees (0 = n'existait pas). */
    public boolean delete(long id) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            redis.zrem("leaderboard:drivers", String.valueOf(id));   // on sort aussi du classement
            return redis.del(key(id)) > 0;
        }
    }
}
