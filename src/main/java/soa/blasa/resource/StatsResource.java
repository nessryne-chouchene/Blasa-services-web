package soa.blasa.resource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.repository.StatsRepository;

/** Ressource /api/stats : classement des conducteurs et compteurs globaux (lecture seule). */
@Path("/stats")
@Produces(MediaType.APPLICATION_JSON)
public class StatsResource {

    private final StatsRepository stats = new StatsRepository();

    /** GET /stats/top-drivers?limit=3 -> 200 (ZREVRANGE sur le classement). */
    @GET
    @Path("/top-drivers")
    public Response topDrivers(@QueryParam("limit") @DefaultValue("3") int limit) {
        if (limit < 1 || limit > 50) {
            return UserResource.error(Response.Status.BAD_REQUEST, "limit doit etre entre 1 et 50");
        }
        return Response.ok(stats.topDrivers(limit)).build();
    }

    /** GET /stats/summary -> 200 (HGETALL des compteurs). */
    @GET
    @Path("/summary")
    public Response summary() {
        return Response.ok(stats.summary()).build();
    }
}
