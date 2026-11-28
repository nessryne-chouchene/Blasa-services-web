package soa.blasa.resource;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import soa.blasa.model.User;
import soa.blasa.repository.UserRepository;
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

    /** Vrai si l'utilisateur connecte est cet utilisateur (toujours faux quand l'authentification est inactive). */
    static boolean isCurrentUser(ContainerRequestContext ctx, long userId) {
        return active(ctx) && ctx.getProperty(AuthFilter.USER_ID_PROPERTY) instanceof Long id && id == userId;
    }

    /** Authentification active : l'email des autres utilisateurs est cache (donnee personnelle). */
    static void hideEmailUnlessSelf(ContainerRequestContext ctx, User u) {
        if (active(ctx) && !(ctx.getProperty(AuthFilter.USER_ID_PROPERTY) instanceof Long id && id.equals(u.getId()))) {
            u.setEmail(null);
        }
    }

    /**
     * Null si permis, sinon 403 : l'utilisateur connecte doit avoir ce role.
     * Un compte sans role (anciens comptes) garde tous les droits.
     */
    static Response requireRole(ContainerRequestContext ctx, UserRepository users, String role, String message) {
        if (!active(ctx) || !(ctx.getProperty(AuthFilter.USER_ID_PROPERTY) instanceof Long id)) {
            return null;
        }
        User u = users.find(id);
        if (u != null && u.getRole() != null && !u.getRole().equals(role)) {
            return UserResource.error(Response.Status.FORBIDDEN, message);
        }
        return null;
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
