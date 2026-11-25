package soa.blasa;

import java.net.URI;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Point d'entree : demarre un petit serveur web (Grizzly) qui sert
 *  - l'API REST sur   http://localhost:8080/api/...
 *  - la page web sur  http://localhost:8080/
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // Jersey scanne ce package pour trouver les classes annotees @Path
        ResourceConfig config = new ResourceConfig()
                .packages("soa.blasa.resource")
                .register(JacksonFeature.class);   // active la conversion JSON

        URI base = URI.create("http://0.0.0.0:8080/api/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(base, config, false);

        // La page web (dossier src/main/resources/web) est servie a la racine "/"
        server.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(Main.class.getClassLoader(), "web/"), "/");

        server.start();
        System.out.println("Blasa demarre : http://localhost:8080/  (API : http://localhost:8080/api)");
        System.out.println("Ctrl+C pour arreter.");
        Thread.currentThread().join();
    }
}
