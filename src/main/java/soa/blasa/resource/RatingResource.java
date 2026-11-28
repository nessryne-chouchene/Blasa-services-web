package soa.blasa.resource;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.model.Role;
import soa.blasa.model.User;
import soa.blasa.repository.StatsRepository;
import soa.blasa.repository.UserRepository;

/** Sous-ressource /api/users/{id}/ratings : noter un conducteur (1 a 5). */
@Path("/users/{id}/ratings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RatingResource {

    private final StatsRepository stats = new StatsRepository();

    /** POST : corps {"score": 5} -> 201 / 400 / 404. */
    @POST
    public Response rate(@PathParam("id") long id, Map<String, Object> body, @Context ContainerRequestContext ctx) {
        Response notPassenger = Authz.requireRole(ctx, new UserRepository(), Role.PASSENGER, "seuls les passagers peuvent noter un conducteur");
        if (notPassenger != null) {
            return notPassenger;
        }
        if (Authz.isCurrentUser(ctx, id)) {
            return UserResource.error(Response.Status.FORBIDDEN, "tu ne peux pas te noter toi-meme");
        }
        Object s = body == null ? null : body.get("score");
        if (!(s instanceof Integer) || (Integer) s < 1 || (Integer) s > 5) {
            return UserResource.error(Response.Status.BAD_REQUEST, "score doit etre un entier entre 1 et 5");
        }
        User u = stats.addRating(id, (Integer) s);
        if (u == null) {
            return UserResource.error(Response.Status.NOT_FOUND, "utilisateur " + id + " introuvable");
        }
        return Response.created(URI.create("/api/users/" + id))
                .entity(Map.of("userId", id, "score", s,
                        "averageRating", u.getAverageRating(), "ratingCount", u.getRatingCount()))
                .build();
    }
}
