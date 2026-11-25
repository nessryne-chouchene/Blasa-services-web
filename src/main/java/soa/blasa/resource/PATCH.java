package soa.blasa.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.HttpMethod;

/**
 * JAX-RS fournit @GET, @POST, @PUT, @DELETE mais PAS @PATCH.
 * On cree donc notre propre annotation, reconnue par Jersey grace a @HttpMethod.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH")
public @interface PATCH {
}
