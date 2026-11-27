package soa.blasa.resource;

import java.time.Instant;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.model.User;
import soa.blasa.repository.UserRepository;
import soa.blasa.security.AuthConfig;
import soa.blasa.security.PasswordHasher;

/** Ressource REST /api/auth : connexion, renvoie un jeton JWT. */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    /** Hash factice : on verifie toujours un mot de passe, pour ne pas reveler si l'email existe. */
    private static final String DUMMY_HASH = PasswordHasher.hash("mot-de-passe-factice");

    private final UserRepository users = new UserRepository();

    /** POST /auth/login : corps {"email": "...", "password": "..."} -> 200 + jeton / 400 / 401. */
    @POST
    @Path("/login")
    public Response login(Map<String, Object> body) {
        Object email = body == null ? null : body.get("email");
        Object password = body == null ? null : body.get("password");
        if (!(email instanceof String e) || e.isBlank() || !(password instanceof String pw) || pw.isEmpty()) {
            return UserResource.error(Response.Status.BAD_REQUEST, "email et password sont obligatoires");
        }
        Long userId = null;
        for (User u : users.findAll()) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(e.trim())) {
                String hash = users.passwordHash(u.getId());
                if (hash != null && PasswordHasher.verify(pw, hash)) {
                    userId = u.getId();
                    break;
                }
            }
        }
        if (userId == null) {
            PasswordHasher.verify(pw, DUMMY_HASH);
            return UserResource.error(Response.Status.UNAUTHORIZED, "email ou mot de passe incorrect");
        }
        String token = AuthConfig.jwt().issue(userId, Instant.now().getEpochSecond());
        return Response.ok(Map.of("token", token, "tokenType", "Bearer",
                "expiresIn", AuthConfig.jwt().ttlSeconds(), "userId", userId)).build();
    }
}
