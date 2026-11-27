package soa.blasa;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import soa.blasa.security.AuthConfig;
import soa.blasa.security.AuthFilter;

/**
 * Point d'entree : demarre un petit serveur web (Grizzly) qui sert
 *  - l'API REST sur   http://localhost:8080/api/...
 *  - la page web sur  http://localhost:8080/
 *  - la documentation OpenAPI (Swagger UI) sur http://localhost:8080/docs.html
 *
 * Options (variables d'environnement, toutes desactivees par defaut) :
 *  - AUTH_ENABLED=true   exige un jeton JWT pour les ecritures (voir README)
 *  - HTTPS_ENABLED=true  sert tout en HTTPS sur le port 8443 (KEYSTORE_PATH, KEYSTORE_PASSWORD ; voir README)
 *  - PORT                port du serveur (defaut 8080, ou 8443 en HTTPS)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        boolean https = "true".equalsIgnoreCase(System.getenv("HTTPS_ENABLED"));
        String scheme = https ? "https" : "http";
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", https ? "8443" : "8080"));

        // Jersey scanne ce package pour trouver les classes annotees @Path
        ResourceConfig config = new ResourceConfig()
                .packages("soa.blasa.resource")
                .register(JacksonFeature.class);   // active la conversion JSON
        if (AuthConfig.enabled()) {
            config.register(AuthFilter.class);     // verifie le jeton JWT sur les ecritures
        }

        URI base = URI.create("http://0.0.0.0:" + port + "/api/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(base, config, false);

        if (https) {
            enableHttps(server);
        }

        // La page web (dossier src/main/resources/web) est servie a la racine "/"
        server.getServerConfiguration().addHttpHandler(
                new CLStaticHttpHandler(Main.class.getClassLoader(), "web/"), "/");

        server.start();
        System.out.println("Blasa demarre : " + scheme + "://localhost:" + port + "/  (API : " + scheme + "://localhost:" + port + "/api)");
        System.out.println("Documentation : " + scheme + "://localhost:" + port + "/docs.html");
        if (AuthConfig.enabled()) {
            System.out.println("Authentification JWT ACTIVE : POST /api/auth/login pour obtenir un jeton.");
        }
        System.out.println("Ctrl+C pour arreter.");
        Thread.currentThread().join();
    }

    /** Passe le serveur en HTTPS avec un certificat PKCS12 (voir README pour le creer avec keytool). */
    private static void enableHttps(HttpServer server) throws Exception {
        String path = System.getenv().getOrDefault("KEYSTORE_PATH", "blasa.p12");
        char[] password = System.getenv().getOrDefault("KEYSTORE_PASSWORD", "changeit").toCharArray();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(path)) {
            keyStore.load(in, password);
        } catch (java.io.FileNotFoundException e) {
            throw new IllegalStateException("Certificat introuvable : " + path
                    + " (voir la section HTTPS du README pour le generer)", e);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, null);

        SSLEngineConfigurator engine = new SSLEngineConfigurator(ssl, false, false, false);
        for (NetworkListener listener : server.getListeners()) {
            listener.setSecure(true);
            listener.setSSLEngineConfig(engine);
        }
    }
}
