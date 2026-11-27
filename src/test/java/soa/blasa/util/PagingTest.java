package soa.blasa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PagingTest {

    private static List<Integer> numbers(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) l.add(i);
        return l;
    }

    @Test
    void secondePage() {
        Paging.Slice<Integer> s = Paging.slice(numbers(45), 2, 20);
        assertEquals(20, s.items().size());
        assertEquals(21, s.items().get(0));
        assertEquals(45, s.total());
        assertEquals(3, s.totalPages());
    }

    @Test
    void dernierePageIncomplete() {
        assertEquals(5, Paging.slice(numbers(45), 3, 20).items().size());
    }

    @Test
    void pageAuDelaDuBoutEstVide() {
        Paging.Slice<Integer> s = Paging.slice(numbers(45), 9, 20);
        assertTrue(s.items().isEmpty());
        assertEquals(3, s.totalPages());
    }

    @Test
    void valeursParDefaut() {
        Paging.Slice<Integer> s = Paging.slice(numbers(5), null, null);
        assertEquals(1, s.page());
        assertEquals(Paging.DEFAULT_SIZE, s.size());
    }

    @Test
    void listeVide() {
        Paging.Slice<Integer> s = Paging.slice(new ArrayList<>(), 1, 10);
        assertTrue(s.items().isEmpty());
        assertEquals(0, s.totalPages());
    }

    @Test
    void parametresInvalides() {
        assertThrows(IllegalArgumentException.class, () -> Paging.slice(numbers(3), 0, 10));
        assertThrows(IllegalArgumentException.class, () -> Paging.slice(numbers(3), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> Paging.slice(numbers(3), 1, Paging.MAX_SIZE + 1));
    }

    @Test
    void pagination_demandee_ou_non() {
        assertFalse(Paging.requested(null, null));
        assertTrue(Paging.requested(1, null));
        assertTrue(Paging.requested(null, 10));
    }
}
