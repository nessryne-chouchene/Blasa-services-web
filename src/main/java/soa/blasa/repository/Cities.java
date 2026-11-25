package soa.blasa.repository;

import java.text.Normalizer;
import java.util.Map;

/**
 * Villes de Tunisie connues : on y trouve les coordonnees (latitude, longitude)
 * pour placer un trajet sur la carte GEO de Redis, et on normalise les noms
 * ("Sfax", "SFAX " -> "sfax") pour que les index soient coherents.
 */
public class Cities {

    private static final Map<String, double[]> COORDS = Map.ofEntries(
            Map.entry("tunis", new double[] { 36.8065, 10.1815 }),
            Map.entry("ariana", new double[] { 36.8665, 10.1647 }),
            Map.entry("bizerte", new double[] { 37.2744, 9.8739 }),
            Map.entry("nabeul", new double[] { 36.4513, 10.7357 }),
            Map.entry("hammamet", new double[] { 36.4000, 10.6167 }),
            Map.entry("zaghouan", new double[] { 36.4029, 10.1429 }),
            Map.entry("beja", new double[] { 36.7256, 9.1817 }),
            Map.entry("jendouba", new double[] { 36.5011, 8.7802 }),
            Map.entry("kef", new double[] { 36.1826, 8.7148 }),
            Map.entry("sousse", new double[] { 35.8256, 10.6411 }),
            Map.entry("monastir", new double[] { 35.7643, 10.8113 }),
            Map.entry("mahdia", new double[] { 35.5047, 11.0622 }),
            Map.entry("kairouan", new double[] { 35.6781, 10.0963 }),
            Map.entry("kasserine", new double[] { 35.1676, 8.8365 }),
            Map.entry("sidibouzid", new double[] { 35.0382, 9.4849 }),
            Map.entry("sfax", new double[] { 34.7406, 10.7600 }),
            Map.entry("gafsa", new double[] { 34.4250, 8.7842 }),
            Map.entry("gabes", new double[] { 33.8815, 10.0982 }),
            Map.entry("medenine", new double[] { 33.3549, 10.5055 }),
            Map.entry("djerba", new double[] { 33.8076, 10.8451 }),
            Map.entry("tozeur", new double[] { 33.9197, 8.1335 }),
            Map.entry("tataouine", new double[] { 32.9297, 10.4518 }));

    /** "  Sfax " -> "sfax" ; "Gabès" -> "gabes" ; "Sidi Bouzid" -> "sidibouzid". */
    public static String slug(String name) {
        if (name == null) {
            return "";
        }
        String s = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase().replaceAll("[^a-z]", "");
    }

    /** {latitude, longitude} ou null si la ville n'est pas connue. */
    public static double[] coords(String slug) {
        return COORDS.get(slug);
    }
}
