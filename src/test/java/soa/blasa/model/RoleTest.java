package soa.blasa.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void valeursValides() {
        assertEquals("DRIVER", Role.normalize("DRIVER"));
        assertEquals("PASSENGER", Role.normalize("PASSENGER"));
    }

    @Test
    void casseEtEspacesIgnores() {
        assertEquals("DRIVER", Role.normalize("  driver "));
        assertEquals("PASSENGER", Role.normalize("Passenger"));
    }

    @Test
    void absentOuVideEstNull() {
        assertNull(Role.normalize(null));
        assertNull(Role.normalize("   "));
    }

    @Test
    void valeurInconnueRefusee() {
        assertThrows(IllegalArgumentException.class, () -> Role.normalize("ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> Role.normalize("conducteur"));
    }
}
