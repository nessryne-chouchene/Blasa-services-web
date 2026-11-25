package soa.blasa.repository;

import redis.clients.jedis.Jedis;

/**
 * Evenements en direct, via le Pub/Sub de Redis.
 *
 *   publish()  : l'API envoie un message sur le canal "blasa:events" (commande PUBLISH)
 *   le canal   : Redis le distribue a tous les abonnes (commande SUBSCRIBE)
 *   (cote abonne : voir EventResource, qui le retransmet aux navigateurs en SSE)
 *
 * Les messages sont du JSON ecrit a la main : seuls des nombres et des noms de villes
 * (lettres seulement) y figurent, il n'y a donc rien a echapper.
 */
public class Events {

    public static final String CHANNEL = "blasa:events";

    /** Envoie un evenement ; une panne du Pub/Sub ne doit jamais faire echouer l'action metier. */
    public static void publish(Jedis redis, String type, long rideId, String extraJson) {
        try {
            String json = "{\"type\":\"" + type + "\",\"rideId\":" + rideId
                    + (extraJson == null || extraJson.isEmpty() ? "" : "," + extraJson) + "}";
            redis.publish(CHANNEL, json);
        } catch (RuntimeException e) {
            // evenement perdu : sans importance pour les donnees
        }
    }
}
