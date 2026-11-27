package soa.blasa.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void bonMotDePasse() {
        assertTrue(PasswordHasher.verify("motdepasse1", PasswordHasher.hash("motdepasse1")));
    }

    @Test
    void mauvaisMotDePasse() {
        assertFalse(PasswordHasher.verify("motdepasse2", PasswordHasher.hash("motdepasse1")));
    }

    @Test
    void chaqueHashA_unSelDifferent() {
        assertNotEquals(PasswordHasher.hash("motdepasse1"), PasswordHasher.hash("motdepasse1"));
    }

    @Test
    void formatInvalide() {
        assertFalse(PasswordHasher.verify("x", "pas un hash"));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("motdepasse1")));
    }
}
