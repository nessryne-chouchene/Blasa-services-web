package soa.blasa.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import soa.blasa.model.User;
import soa.blasa.security.PasswordHasher;

class UserRepositoryTest {

    private final UserRepository repo = new UserRepository();

    @BeforeEach
    void setUp() {
        RedisTestSupport.prepare();
    }

    private User user(String name, String email, String city) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setCity(city);
        return u;
    }

    @Test
    void creerPuisLire() {
        User created = repo.create(user("Sarra", "sarra@mail.tn", "Sfax"));
        assertNotNull(created.getId());
        User found = repo.find(created.getId());
        assertEquals("Sarra", found.getName());
        assertEquals("sfax", found.getCity());          // la ville est stockee en minuscules
        assertEquals(0, found.getRatingCount());
    }

    @Test
    void lesIdentifiantsSontUniquesEtCroissants() {
        long a = repo.create(user("A", "a@mail.tn", "Tunis")).getId();
        long b = repo.create(user("B", "b@mail.tn", "Tunis")).getId();
        assertTrue(b > a);
    }

    @Test
    void utilisateurInconnu() {
        assertNull(repo.find(9999));
    }

    @Test
    void listeTrieeParId() {
        long a = repo.create(user("A", "a@mail.tn", "Tunis")).getId();
        long b = repo.create(user("B", "b@mail.tn", "Sfax")).getId();
        List<User> all = repo.findAll();
        assertEquals(2, all.size());
        assertEquals(a, all.get(0).getId());
        assertEquals(b, all.get(1).getId());
    }

    @Test
    void modificationPartielleGardeLesAutresChamps() {
        long id = repo.create(user("Sarra", "sarra@mail.tn", "Sfax")).getId();
        User u = repo.update(id, Map.of("city", "Tunis"));
        assertEquals("tunis", u.getCity());
        assertEquals("Sarra", u.getName());
        assertNull(repo.update(9999, Map.of("city", "Tunis")));
    }

    @Test
    void supprimer() {
        long id = repo.create(user("Sarra", "sarra@mail.tn", "Sfax")).getId();
        assertTrue(repo.delete(id));
        assertNull(repo.find(id));
        assertFalse(repo.delete(id));                    // deja supprime
    }

    @Test
    void motDePasseStockeSousFormeDeHash() {
        User u = user("Sarra", "sarra@mail.tn", "Sfax");
        u.setPassword("motdepasse1");
        long id = repo.create(u).getId();
        String hash = repo.passwordHash(id);
        assertNotNull(hash);
        assertFalse(hash.contains("motdepasse1"));
        assertTrue(PasswordHasher.verify("motdepasse1", hash));
    }

    @Test
    void compteSansMotDePasse() {
        long id = repo.create(user("Sarra", "sarra@mail.tn", "Sfax")).getId();
        assertNull(repo.passwordHash(id));
        assertTrue(repo.setPassword(id, "nouveaumdp1"));
        assertTrue(PasswordHasher.verify("nouveaumdp1", repo.passwordHash(id)));
        assertFalse(repo.setPassword(9999, "nouveaumdp1"));
    }
}
