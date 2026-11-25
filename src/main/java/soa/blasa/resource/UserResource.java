package soa.blasa.resource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import soa.blasa.model.User;
import soa.blasa.repository.UserRepository;

/** Ressource REST /api/users : les 5 methodes HTTP du cours sur un meme objet. */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserRepository repo = new UserRepository();

    static Response error(Response.Status status, String message) {
        return Response.status(status).entity(Map.of("error", message)).build();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** POST /users : creer un compte -> 201 Created (+ en-tete Location) ou 400. */
    @POST
    public Response create(User body) {
        if (body == null || blank(body.getName()) || blank(body.getEmail()) || blank(body.getCity())) {
            return error(Response.Status.BAD_REQUEST, "name, email et city sont obligatoires");
        }
        User created = repo.create(body);
        return Response.created(URI.create("/api/users/" + created.getId())).entity(created).build();
    }

    /** GET /users : liste de tous les utilisateurs -> 200. */
    @GET
    public Response list() {
        return Response.ok(repo.findAll()).build();
    }

    /** GET /users/{id} : lire un profil -> 200 ou 404. */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        User u = repo.find(id);
        if (u == null) {
            return error(Response.Status.NOT_FOUND, "utilisateur " + id + " introuvable");
        }
        return Response.ok(u).build();
    }

    /** PUT /users/{id} : remplacement COMPLET (les 3 champs sont obligatoires). */
    @PUT
    @Path("/{id}")
    public Response replace(@PathParam("id") long id, User body) {
        if (body == null || blank(body.getName()) || blank(body.getEmail()) || blank(body.getCity())) {
            return error(Response.Status.BAD_REQUEST, "PUT exige name, email et city");
        }
        Map<String, String> fields = new HashMap<>();
        fields.put("name", body.getName());
        fields.put("email", body.getEmail());
        fields.put("city", body.getCity());
        User u = repo.update(id, fields);
        if (u == null) {
            return error(Response.Status.NOT_FOUND, "utilisateur " + id + " introuvable");
        }
        return Response.ok(u).build();
    }

    /** PATCH /users/{id} : modification PARTIELLE (seulement les champs envoyes). */
    @PATCH
    @Path("/{id}")
    public Response patch(@PathParam("id") long id, Map<String, String> changes) {
        if (changes == null || changes.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "aucun champ a modifier");
        }
        Map<String, String> fields = new HashMap<>();
        for (String f : new String[] { "name", "email", "city" }) {   // seuls ces champs sont modifiables
            if (changes.containsKey(f)) {
                if (blank(changes.get(f))) {
                    return error(Response.Status.BAD_REQUEST, f + " ne peut pas etre vide");
                }
                fields.put(f, changes.get(f));
            }
        }
        if (fields.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "champs modifiables : name, email, city");
        }
        User u = repo.update(id, fields);
        if (u == null) {
            return error(Response.Status.NOT_FOUND, "utilisateur " + id + " introuvable");
        }
        return Response.ok(u).build();
    }

    /** DELETE /users/{id} : supprimer -> 204 No Content (pas de corps) ou 404. */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        if (!repo.delete(id)) {
            return error(Response.Status.NOT_FOUND, "utilisateur " + id + " introuvable");
        }
        return Response.noContent().build();
    }
}
