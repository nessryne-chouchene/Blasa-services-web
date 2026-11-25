package soa.blasa.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;
import soa.blasa.model.Ride;

/**
 * Acces a Redis pour les trajets. Cles utilisees :
 *   ride:{id}              HASH  la fiche du trajet (avec TTL : disparait au depart)
 *   ride:{id}:passengers   SET   ids des passagers
 *   rides:geo              GEO   position de depart de chaque trajet
 *   rides:from:{ville}     SET   index "trajets au depart de..."
 *   rides:to:{ville}       SET   index "trajets a destination de..."
 *   stats:summary          HASH  compteurs
 */
public class RideRepository {

    private static final String GEO = "rides:geo";

    private static String key(long id) { return "ride:" + id; }
    private static String passengersKey(long id) { return "ride:" + id + ":passengers"; }
    private static long idOf(String member) { return Long.parseLong(member.substring("ride:".length())); }

    // ---------------------------------------------------------------- POST
    public Ride create(Ride r, double lat, double lon) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            long id = redis.incr("seq:ride");                       // id unique et atomique
            Map<String, String> f = new HashMap<>();
            f.put("driverId", String.valueOf(r.getDriverId()));
            f.put("origin", r.getOrigin());
            f.put("destination", r.getDestination());
            f.put("departureAt", String.valueOf(r.getDepartureAt()));
            f.put("seats", String.valueOf(r.getSeats()));
            f.put("seatsLeft", String.valueOf(r.getSeats()));
            f.put("price", String.valueOf(r.getPrice()));
            f.put("status", "OPEN");
            redis.hset(key(id), f);
            redis.expireAt(key(id), r.getDepartureAt());            // le trajet s'efface tout seul au depart
            redis.geoadd(GEO, lon, lat, "ride:" + id);              // attention : longitude d'abord
            redis.sadd("rides:from:" + r.getOrigin(), "ride:" + id);
            redis.sadd("rides:to:" + r.getDestination(), "ride:" + id);
            redis.hincrBy("stats:summary", "ridesCreated", 1);
            Events.publish(redis, "ride_created", id,
                    "\"origin\":\"" + r.getOrigin() + "\",\"destination\":\"" + r.getDestination() + "\"");
            return find(id);
        }
    }

    // ----------------------------------------------------------------- GET
    /** Un trajet avec la liste de ses passagers, ou null s'il n'existe pas (ou a expire). */
    public Ride find(long id) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            Ride r = load(redis, id);
            if (r != null) {
                List<Long> passengers = new ArrayList<>();
                for (String p : redis.smembers(passengersKey(id))) {
                    passengers.add(Long.parseLong(p));
                }
                r.setPassengers(passengers);
            }
            return r;
        }
    }

    private Ride load(Jedis redis, long id) {
        Map<String, String> h = redis.hgetAll(key(id));
        if (h.isEmpty()) {
            return null;
        }
        Ride r = new Ride();
        r.setId(id);
        r.setDriverId(Long.parseLong(h.get("driverId")));
        r.setOrigin(h.get("origin"));
        r.setDestination(h.get("destination"));
        r.setDepartureAt(Long.parseLong(h.get("departureAt")));
        r.setSeats(Integer.parseInt(h.get("seats")));
        r.setSeatsLeft(Integer.parseInt(h.get("seatsLeft")));
        r.setPrice(Integer.parseInt(h.get("price")));
        r.setStatus(h.get("status"));
        return r;
    }

    /**
     * Recherche :
     *  - lat+lon          -> GEOSEARCH (trajets autour de moi, tries par distance)
     *  - from et/ou to    -> SINTER / SMEMBERS sur les index (filtre par ville)
     *  - rien             -> tous les trajets (un GEO est un ZSET : ZRANGE 0 -1)
     * Nettoyage "paresseux" : si un trajet a expire (TTL), on le retire des index au passage.
     */
    public List<Ride> search(Double lat, Double lon, double radiusKm, String from, String to) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            List<Ride> out = new ArrayList<>();

            if (lat != null && lon != null) {
                List<GeoRadiusResponse> found = redis.geosearch(GEO, GeoSearchParam.geoSearchParam()
                        .fromLonLat(lon, lat).byRadius(radiusKm, GeoUnit.KM).asc().withDist());
                for (GeoRadiusResponse g : found) {
                    String member = g.getMemberByString();
                    Ride r = load(redis, idOf(member));
                    if (r == null) {
                        redis.zrem(GEO, member);
                        continue;
                    }
                    if (from != null && !from.equals(r.getOrigin())) continue;
                    if (to != null && !to.equals(r.getDestination())) continue;
                    r.setDistanceKm(Math.round(g.getDistance() * 100.0) / 100.0);
                    out.add(r);
                }
                return out;                                          // deja tries par distance
            }

            Set<String> members;
            List<String> indexKeys = new ArrayList<>();
            if (from != null) indexKeys.add("rides:from:" + from);
            if (to != null) indexKeys.add("rides:to:" + to);
            if (indexKeys.size() == 2) {
                members = redis.sinter(indexKeys.get(0), indexKeys.get(1));   // intersection = les deux filtres
            } else if (indexKeys.size() == 1) {
                members = redis.smembers(indexKeys.get(0));
            } else {
                members = new HashSet<>(redis.zrange(GEO, 0, -1));
            }
            for (String member : members) {
                Ride r = load(redis, idOf(member));
                if (r == null) {
                    redis.zrem(GEO, member);
                    for (String k : indexKeys) redis.srem(k, member);
                    continue;
                }
                out.add(r);
            }
            out.sort(Comparator.comparing(Ride::getDepartureAt));
            return out;
        }
    }

    // ----------------------------------------------------- PUT / PATCH
    /** Modifie price / seats / departureAt / status (valeurs deja validees). null si le trajet n'existe pas. */
    public Ride update(long id, Map<String, String> changes) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            if (!redis.exists(key(id))) {
                return null;
            }
            Map<String, String> f = new HashMap<>(changes);
            if (f.containsKey("seats")) {
                int newSeats = Integer.parseInt(f.get("seats"));
                long booked = redis.scard(passengersKey(id));
                if (newSeats < booked) {
                    throw new Conflict("deja " + booked + " reservations : seats ne peut pas etre inferieur");
                }
                f.put("seatsLeft", String.valueOf(newSeats - booked));
            }
            redis.hset(key(id), f);
            if (f.containsKey("departureAt")) {
                long when = Long.parseLong(f.get("departureAt"));
                redis.expireAt(key(id), when);
                redis.expireAt(passengersKey(id), when);
            }
            Events.publish(redis, "ride_updated", id, null);
            return find(id);
        }
    }

    // -------------------------------------------------------------- DELETE
    public boolean delete(long id) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            Map<String, String> h = redis.hgetAll(key(id));
            if (h.isEmpty()) {
                return false;
            }
            String member = "ride:" + id;
            redis.srem("rides:from:" + h.get("origin"), member);
            redis.srem("rides:to:" + h.get("destination"), member);
            redis.zrem(GEO, member);
            redis.del(key(id), passengersKey(id));
            Events.publish(redis, "ride_deleted", id, null);
            return true;
        }
    }

    // ------------------------------------------------------- reservations
    /**
     * Reserve une place. Retourne le nombre de places restantes.
     * HINCRBY est atomique : deux personnes qui reservent en meme temps ne peuvent pas depasser le stock.
     */
    public long book(long rideId, long userId) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            if (!"OPEN".equals(redis.hget(key(rideId), "status"))) {
                throw new Conflict("ce trajet n'est pas ouvert aux reservations");
            }
            if (redis.sismember(passengersKey(rideId), String.valueOf(userId))) {
                throw new Conflict("tu as deja reserve ce trajet");
            }
            long left = redis.hincrBy(key(rideId), "seatsLeft", -1);
            if (left < 0) {
                redis.hincrBy(key(rideId), "seatsLeft", 1);          // on annule : il n'y avait plus de place
                throw new Conflict("trajet complet");
            }
            redis.sadd(passengersKey(rideId), String.valueOf(userId));
            String dep = redis.hget(key(rideId), "departureAt");
            if (dep != null) {
                redis.expireAt(passengersKey(rideId), Long.parseLong(dep));
            }
            redis.hincrBy("stats:summary", "bookings", 1);
            Events.publish(redis, "booked", rideId, "\"userId\":" + userId + ",\"seatsLeft\":" + left);
            return left;
        }
    }

    /** Annule une reservation. false si ce passager n'avait pas reserve. */
    public boolean cancelBooking(long rideId, long userId) {
        try (Jedis redis = RedisConfig.pool().getResource()) {
            if (redis.srem(passengersKey(rideId), String.valueOf(userId)) == 0) {
                return false;
            }
            long left = redis.hincrBy(key(rideId), "seatsLeft", 1);  // la place est rendue
            Events.publish(redis, "booking_cancelled", rideId, "\"userId\":" + userId + ",\"seatsLeft\":" + left);
            return true;
        }
    }
}
