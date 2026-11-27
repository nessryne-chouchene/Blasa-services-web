package soa.blasa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagination en memoire, sans dependance : decoupe une liste en pages.
 * Les parametres sont comptes a partir de 1 (page=1 est la premiere page).
 */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() { }

    /** Une page de resultats + les infos pour naviguer. */
    public record Slice<T>(List<T> items, int page, int size, long total, int totalPages) { }

    /** Le client a-t-il demande la pagination ? Sinon on renvoie la liste complete (comportement historique). */
    public static boolean requested(Integer page, Integer size) {
        return page != null || size != null;
    }

    /**
     * @throws IllegalArgumentException si page &lt; 1 ou si size n'est pas entre 1 et {@link #MAX_SIZE}
     */
    public static <T> Slice<T> slice(List<T> all, Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 1) {
            throw new IllegalArgumentException("page doit etre >= 1");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw new IllegalArgumentException("size doit etre entre 1 et " + MAX_SIZE);
        }
        long total = all.size();
        int from = (int) Math.min((long) (p - 1) * s, total);
        int to = (int) Math.min((long) from + s, total);
        int totalPages = (int) ((total + s - 1) / s);
        return new Slice<>(new ArrayList<>(all.subList(from, to)), p, s, total, totalPages);
    }
}
