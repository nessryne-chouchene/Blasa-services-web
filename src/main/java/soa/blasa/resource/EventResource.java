package soa.blasa.resource;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import soa.blasa.repository.Events;
import soa.blasa.repository.RedisConfig;

/**
 * GET /api/events : flux en direct (Server-Sent Events).
 *
 * Le navigateur ouvre UNE connexion HTTP qui reste ouverte ; le serveur y pousse un message
 * a chaque evenement (reservation, nouveau trajet...). Les messages viennent du Pub/Sub de Redis :
 *
 *   API (PUBLISH) --> Redis (canal blasa:events) --> ce thread (SUBSCRIBE) --> tous les navigateurs
 */
@Singleton
@Path("/events")
public class EventResource {

    private Sse sse;
    private SseBroadcaster broadcaster;
    private boolean started = false;

    @Context
    public void setSse(Sse sse) {
        this.sse = sse;
        this.broadcaster = sse.newBroadcaster();
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sink) {
        startOnce();
        broadcaster.register(sink);
        sink.send(sse.newEventBuilder().name("hello").data("connecte").build());
    }

    /** Demarre (une seule fois) l'ecoute du canal Redis et un "ping" regulier pour garder la connexion ouverte. */
    private synchronized void startOnce() {
        if (started) {
            return;
        }
        started = true;

        Thread listener = new Thread(() -> {
            while (true) {                                           // se reconnecte si Redis redemarre
                try (Jedis redis = new Jedis(RedisConfig.HOST, RedisConfig.PORT)) {
                    redis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            OutboundSseEvent ev = sse.newEventBuilder().name("blasa").data(message).build();
                            broadcaster.broadcast(ev);
                        }
                    }, Events.CHANNEL);
                } catch (RuntimeException e) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { return; }
                }
            }
        }, "blasa-pubsub-listener");
        listener.setDaemon(true);
        listener.start();

        ScheduledExecutorService ping = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "blasa-sse-ping");
            t.setDaemon(true);
            return t;
        });
        ping.scheduleAtFixedRate(() -> broadcaster.broadcast(sse.newEventBuilder().name("ping").data("ok").build()),
                20, 20, TimeUnit.SECONDS);
    }
}
