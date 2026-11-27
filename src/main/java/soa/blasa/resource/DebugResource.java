package soa.blasa.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import soa.blasa.repository.RedisConfig;

/**
 * Outil de demonstration (lecture seule) : montre le contenu reel de Redis
 * dans l'onglet "Sous le capot" de la page web. Ne fait partie de l'API metier.
 */
@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    /** GET /debug/redis : toutes les cles avec type, TTL et contenu. */
    @GET
    @Path("/redis")
    public Response dump() {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            TreeSet<String> keys = new TreeSet<>();
            String cursor = ScanParams.SCAN_POINTER_START;
            do {                                                     // SCAN parcourt les cles par petits paquets
                ScanResult<String> page = redis.scan(cursor, new ScanParams().count(100));
                keys.addAll(page.getResult());
                cursor = page.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));

            List<Map<String, Object>> out = new ArrayList<>();
            for (String k : keys) {
                Map<String, Object> row = new LinkedHashMap<>();
                String type = redis.type(k);
                row.put("key", k);
                row.put("type", k.equals("rides:geo") ? "geo" : type);   // un GEO est stocke comme un zset
                row.put("ttl", redis.ttl(k));
                row.put("value", value(redis, k, type));
                out.add(row);
            }
            return Response.ok(out).build();
        }
    }

    private Object value(Jedis redis, String key, String type) {
        switch (type) {
            case "string":
                return redis.get(key);
            case "hash":
                Map<String, String> fields = new java.util.TreeMap<>(redis.hgetAll(key));
                if (fields.containsKey("pwHash")) {
                    fields.put("pwHash", "***");                 // le hash du mot de passe n'est jamais expose
                }
                return fields;
            case "set":
                return new TreeSet<>(redis.smembers(key));
            case "zset":
                Map<String, Object> m = new LinkedHashMap<>();
                List<Tuple> tuples = redis.zrangeWithScores(key, 0, -1);
                if (key.equals("rides:geo")) {
                    String[] members = tuples.stream().map(Tuple::getElement).toArray(String[]::new);
                    List<GeoCoordinate> pos = redis.geopos(key, members);
                    for (int i = 0; i < members.length; i++) {
                        GeoCoordinate p = pos.get(i);
                        m.put(members[i], String.format(java.util.Locale.ROOT, "lat %.4f, lon %.4f",
                                p.getLatitude(), p.getLongitude()));
                    }
                } else {
                    for (Tuple t : tuples) {
                        m.put(t.getElement(), t.getScore());
                    }
                }
                return m;
            default:
                return null;
        }
    }
}
