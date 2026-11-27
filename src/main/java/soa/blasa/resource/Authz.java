package soa.blasa.resource;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import soa.blasa.security.AuthFilter;

/**
 * Controles de propriete, utilises seulement quand l'authentification est active
 * (AuthFilter a alors place l'id de l'utilisateur connecte dans la requete).
 * Sans authentification, tout reste permis comme avant.
 */
final class Authz {

    private Authz() { }

    /** Vrai si un utilisateur est connecte (AUTH_ENABLED=true et jeton valide). */
    static boolean active(ContainerRequestContext ctx) {
        return ctx != null && ctx.getProperty(AuthFilter.USER_ID_PROPERTY) != null;
    }

    /** Null si l'action est permise, sinon une reponse 403. */
    static Response denyUnlessUser(ContainerRequestContext ctx, Long expectedUserId) {
        if (!active(ctx)) {
            return null;
        }
        Object current = ctx.getProperty(AuthFilter.USER_ID_PROPERTY);
        if (current instanceof Long id && id.equals(expectedUserId)) {
            return null;
        }
        return UserResource.error(Response.Status.FORBIDDEN, "action reservee au proprietaire de la ressource");
    }
}
