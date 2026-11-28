package soa.blasa.resource;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.model.Ride;
import soa.blasa.model.Role;
import soa.blasa.repository.Conflict;
import soa.blasa.repository.RideRepository;
import soa.blasa.repository.UserRepository;

/** Sous-ressource /api/rides/{rideId}/bookings : reserver et annuler une place. */
@Path("/rides/{rideId}/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    private final RideRepository rides = new RideRepository();
    private final UserRepository users = new UserRepository();

    /** POST : reserver (corps : {"userId": 3}) -> 201 / 400 / 404 / 409. */
    @POST
    public Response book(@PathParam("rideId") long rideId, Map<String, Object> body,
            @Context ContainerRequestContext ctx) {
        if (body == null || !(body.get("userId") instanceof Number)) {
            return UserResource.error(Response.Status.BAD_REQUEST, "userId (nombre) est obligatoire");
        }
        long userId = ((Number) body.get("userId")).longValue();
        Response denied = Authz.denyUnlessUser(ctx, userId);      // on ne reserve qu'en son nom (si auth active)
        if (denied != null) {
            return denied;
        }
        Response notPassenger = Authz.requireRole(ctx, users, Role.PASSENGER, "seuls les passagers peuvent reserver une place");
        if (notPassenger != null) {
            return notPassenger;
        }

        Ride ride = rides.find(rideId);
        if (ride == null) {
            return UserResource.error(Response.Status.NOT_FOUND, "trajet " + rideId + " introuvable");
        }
        if (users.find(userId) == null) {
            return UserResource.error(Response.Status.NOT_FOUND, "utilisateur " + userId + " introuvable");
        }
        if (ride.getDriverId() == userId) {
            return UserResource.error(Response.Status.BAD_REQUEST, "le conducteur ne peut pas reserver son propre trajet");
        }
        try {
            long left = rides.book(rideId, userId);
            return Response.created(URI.create("/api/rides/" + rideId))
                    .entity(Map.of("rideId", rideId, "userId", userId, "seatsLeft", left)).build();
        } catch (Conflict c) {
            return UserResource.error(Response.Status.CONFLICT, c.getMessage());
        }
    }

    /** DELETE /rides/{rideId}/bookings/{userId} : annuler -> 204 / 404. */
    @DELETE
    @Path("/{userId}")
    public Response cancel(@PathParam("rideId") long rideId, @PathParam("userId") long userId) {
        if (rides.find(rideId) == null) {
            return UserResource.error(Response.Status.NOT_FOUND, "trajet " + rideId + " introuvable");
        }
        if (!rides.cancelBooking(rideId, userId)) {
            return UserResource.error(Response.Status.NOT_FOUND, "pas de reservation de l'utilisateur " + userId + " sur ce trajet");
        }
        return Response.noContent().build();
    }
}
