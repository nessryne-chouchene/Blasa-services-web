package soa.blasa.model;

import java.util.Locale;

/**
 * Role choisi a l'inscription :
 *  PASSENGER : "je cherche un covoiturage"            -> peut reserver, ne peut pas proposer de trajet
 *  DRIVER    : "j'ai une voiture, je cherche des passagers" -> peut proposer des trajets, ne reserve pas
 * Un ancien compte sans role (null) garde tous les droits.
 */
public final class Role {

    public static final String PASSENGER = "PASSENGER";
    public static final String DRIVER = "DRIVER";

    private Role() { }

    /** "driver" -> "DRIVER" ; vide -> null ; autre valeur -> IllegalArgumentException. */
    public static String normalize(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String r = role.trim().toUpperCase(Locale.ROOT);
        if (r.equals(PASSENGER) || r.equals(DRIVER)) {
            return r;
        }
        throw new IllegalArgumentException("role doit etre PASSENGER ou DRIVER");
    }
}
