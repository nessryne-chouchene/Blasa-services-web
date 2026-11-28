package soa.blasa.resource;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.model.Ride;
import soa.blasa.model.Role;
import soa.blasa.repository.Cities;
import soa.blasa.repository.Conflict;
import soa.blasa.repository.RideRepository;
import soa.blasa.repository.UserRepository;

/** Ressource REST /api/rides : les trajets (CRUD + recherche). */
@Path("/rides")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RideResource {

    private final RideRepository rides = new RideRepository();
    private final UserRepository users = new UserRepository();

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static long now() {
        return Instant.now().getEpochSecond();
    }

    /** Si l'authentification est active : seul le conducteur du trajet peut le modifier ou l'annuler. */
    private Response denyUnlessDriver(ContainerRequestContext ctx, long rideId) {
        if (!Authz.active(ctx)) {
            return null;
        }
        Ride r = rides.find(rideId);
        return r == null ? null : Authz.denyUnlessUser(ctx, r.getDriverId());   // trajet inconnu : le 404 suivra
    }

    /** POST /rides : proposer un trajet -> 201 / 400 / 404 (conducteur inconnu). */
    @POST
    public Response create(Ride b, @Context ContainerRequestContext ctx) {
        if (b != null && b.getDriverId() != null) {
            Response denied = Authz.denyUnlessUser(ctx, b.getDriverId());   // on ne propose un trajet qu'en son nom
            if (denied != null) {
                return denied;
            }
        }
        Response notDriver = Authz.requireRole(ctx, users, Role.DRIVER, "seuls les conducteurs peuvent proposer un trajet");
        if (notDriver != null) {
            return notDriver;
        }
        if (b == null || b.getDriverId() == null || blank(b.getOrigin()) || blank(b.getDestination())
                || b.getDepartureAt() == null || b.getSeats() == null || b.getPrice() == null) {
            return UserResource.error(Response.Status.BAD_REQUEST,
                    "driverId, origin, destination, departureAt, seats et price sont obligatoires");
        }
        String origin = Cities.slug(b.getOrigin());
        String destination = Cities.slug(b.getDestination());
        if (origin.isEmpty() || destination.isEmpty() || origin.equals(destination)) {
            return UserResource.error(Response.Status.BAD_REQUEST, "origin et destination doivent etre deux villes differentes");
        }
        if (b.getSeats() < 1 || b.getSeats() > 8) {
            return UserResource.error(Response.Status.BAD_REQUEST, "seats doit etre entre 1 et 8");
        }
        if (b.getPrice() < 0) {
            return UserResource.error(Response.Status.BAD_REQUEST, "price ne peut pas etre negatif");
        }
        if (b.getDepartureAt() <= now()) {
            return UserResource.error(Response.Status.BAD_REQUEST, "departureAt doit etre dans le futur");
        }
        Double lat = b.getOriginLat();
        Double lon = b.getOriginLon();
        if (lat == null || lon == null) {
            double[] c = Cities.coords(origin);
            if (c == null) {
                return UserResource.error(Response.Status.BAD_REQUEST,
                        "ville de depart inconnue : donne originLat et originLon");
            }
            lat = c[0];
            lon = c[1];
        }
        if (users.find(b.getDriverId()) == null) {
            return UserResource.error(Response.Status.NOT_FOUND, "conducteur " + b.getDriverId() + " introuvable");
        }
        b.setOrigin(origin);
        b.setDestination(destination);
        Ride created = rides.create(b, lat, lon);
        return Response.created(URI.create("/api/rides/" + created.getId())).entity(created).build();
    }

    /**
     * GET /rides : chercher des trajets.
     *   ?lat=..&lon=..&radius=50   autour d'un point (GEO)
     *   ?from=sfax&to=tunis        par villes (index SET)
     *   (sans parametre)           tous les trajets
     * Pagination optionnelle (se combine avec tous les modes) : &page=1&size=20
     */
    @GET
    public Response search(@QueryParam("lat") Double lat, @QueryParam("lon") Double lon,
            @QueryParam("radius") @DefaultValue("50") double radius,
            @QueryParam("from") String from, @QueryParam("to") String to,
            @QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        if ((lat == null) != (lon == null)) {
            return UserResource.error(Response.Status.BAD_REQUEST, "lat et lon vont ensemble");
        }
        if (radius <= 0) {
            return UserResource.error(Response.Status.BAD_REQUEST, "radius doit etre positif");
        }
        List<Ride> found = rides.search(lat, lon, radius,
                blank(from) ? null : Cities.slug(from), blank(to) ? null : Cities.slug(to));
        return PagedResponse.of(found, page, size);       // sans page/size : liste complete, comme avant
    }

    /** GET /rides/{id} : detail (avec la liste des passagers) -> 200 / 404. */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        Ride r = rides.find(id);
        if (r == null) {
            return UserResource.error(Response.Status.NOT_FOUND, "trajet " + id + " introuvable");
        }
        return Response.ok(r).build();
    }

    /** PUT /rides/{id} : remplacer price + seats + departureAt (les trois sont obligatoires). */
    @PUT
    @Path("/{id}")
    public Response replace(@PathParam("id") long id, Ride b, @Context ContainerRequestContext ctx) {
        Response denied = denyUnlessDriver(ctx, id);
        if (denied != null) {
            return denied;
        }
        if (b == null || b.getPrice() == null || b.getSeats() == null || b.getDepartureAt() == null) {
            return UserResource.error(Response.Status.BAD_REQUEST, "PUT exige price, seats et departureAt");
        }
        Map<String, String> in = new HashMap<>();
        in.put("price", String.valueOf(b.getPrice()));
        in.put("seats", String.valueOf(b.getSeats()));
        in.put("departureAt", String.valueOf(b.getDepartureAt()));
        return apply(id, in);
    }

    /** PATCH /rides/{id} : modifier seulement certains champs (price, seats, departureAt, status). */
    @PATCH
    @Path("/{id}")
    public Response patch(@PathParam("id") long id, Map<String, String> changes, @Context ContainerRequestContext ctx) {
        Response denied = denyUnlessDriver(ctx, id);
        if (denied != null) {
            return denied;
        }
        if (changes == null || changes.isEmpty()) {
            return UserResource.error(Response.Status.BAD_REQUEST, "aucun champ a modifier");
        }
        return apply(id, changes);
    }

    /** Valide les champs puis les ecrit (commun a PUT et PATCH). */
    private Response apply(long id, Map<String, String> in) {
        Map<String, String> ok = new HashMap<>();
        try {
            for (Map.Entry<String, String> e : in.entrySet()) {
                String v = e.getValue() == null ? "" : e.getValue().trim();
                switch (e.getKey()) {
                    case "price" -> {
                        if (Integer.parseInt(v) < 0) throw new IllegalArgumentException("price negatif");
                        ok.put("price", v);
                    }
                    case "seats" -> {
                        int s = Integer.parseInt(v);
                        if (s < 1 || s > 8) throw new IllegalArgumentException("seats entre 1 et 8");
                        ok.put("seats", v);
                    }
                    case "departureAt" -> {
                        if (Long.parseLong(v) <= now()) throw new IllegalArgumentException("departureAt dans le futur");
                        ok.put("departureAt", v);
                    }
                    case "status" -> {
                        if (!v.equals("OPEN") && !v.equals("CLOSED")) throw new IllegalArgumentException("status : OPEN ou CLOSED");
                        ok.put("status", v);
                    }
                    default -> { /* champ non modifiable : ignore */ }
                }
            }
        } catch (IllegalArgumentException ex) {          // inclut NumberFormatException
            return UserResource.error(Response.Status.BAD_REQUEST, "valeur invalide : " + ex.getMessage());
        }
        if (ok.isEmpty()) {
            return UserResource.error(Response.Status.BAD_REQUEST, "champs modifiables : price, seats, departureAt, status");
        }
        try {
            Ride r = rides.update(id, ok);
            if (r == null) {
                return UserResource.error(Response.Status.NOT_FOUND, "trajet " + id + " introuvable");
            }
            return Response.ok(r).build();
        } catch (Conflict c) {
            return UserResource.error(Response.Status.CONFLICT, c.getMessage());
        }
    }

    /** DELETE /rides/{id} : annuler le trajet -> 204 / 404. */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id, @Context ContainerRequestContext ctx) {
        Response denied = denyUnlessDriver(ctx, id);
        if (denied != null) {
            return denied;
        }
        if (!rides.delete(id)) {
            return UserResource.error(Response.Status.NOT_FOUND, "trajet " + id + " introuvable");
        }
        return Response.noContent().build();
    }
}
