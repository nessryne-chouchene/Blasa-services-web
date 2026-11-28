package soa.blasa.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.repository.UserRepository;

/**
 * Filtre JWT, enregistre dans Main seulement si AUTH_ENABLED=true.
 * Regles :
 *  - POST /users (inscription), POST /auth/login et GET /events (flux en direct : EventSource ne sait pas
 *    envoyer d'en-tete) sont publics ;
 *  - TOUT le reste, lectures comprises, exige "Authorization: Bearer <jeton>" d'un compte qui existe encore (sinon 401) ;
 *  - on ne peut modifier / supprimer que son propre compte et annuler que ses propres reservations (sinon 403).
 * Les controles "proprietaire du trajet" et "role" sont faits dans les ressources (propriete "userId").
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    public static final String USER_ID_PROPERTY = "userId";

    private static final Pattern USER_PATH = Pattern.compile("^users/(\\d{1,18})$");
    private static final Pattern BOOKING_PATH = Pattern.compile("^rides/\\d{1,18}/bookings/(\\d{1,18})$");

    private final UserRepository users = new UserRepository();

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String method = ctx.getMethod();
        if (method.equals("OPTIONS")) {
            return;
        }
        String path = ctx.getUriInfo().getPath().replaceAll("^/+|/+$", "");
        boolean read = method.equals("GET") || method.equals("HEAD");
        if ((read && path.equals("events"))
                || (method.equals("POST") && (path.equals("users") || path.equals("auth/login")))) {
            return;
        }

        String header = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        Long userId = null;
        if (header != null && header.startsWith("Bearer ")) {
            userId = AuthConfig.jwt().verify(header.substring("Bearer ".length()).trim(),
                    Instant.now().getEpochSecond());
        }
        if (userId != null && users.find(userId) == null) {
            userId = null;                                   // compte supprime : le jeton ne vaut plus rien
        }
        if (userId == null) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", "connexion requise : POST /api/auth/login puis en-tete Authorization: Bearer <jeton>"))
                    .build());
            return;
        }

        if (!read) {
            Matcher own = USER_PATH.matcher(path);
            Matcher booking = BOOKING_PATH.matcher(path);
            if ((own.matches() && Long.parseLong(own.group(1)) != userId)
                    || (booking.matches() && Long.parseLong(booking.group(1)) != userId)) {
                ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(Map.of("error", "action reservee au proprietaire de la ressource"))
                        .build());
                return;
            }
        }
        ctx.setProperty(USER_ID_PROPERTY, userId);
    }
}
