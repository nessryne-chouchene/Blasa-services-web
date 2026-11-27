package soa.blasa.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import soa.blasa.model.Ride;
import soa.blasa.model.User;

class RideRepositoryTest {

    private final RideRepository rides = new RideRepository();
    private final UserRepository users = new UserRepository();
    private long driver;
    private long passenger1;
    private long passenger2;

    @BeforeEach
    void setUp() {
        RedisTestSupport.prepare();
        driver = newUser("Conducteur");
        passenger1 = newUser("Passager 1");
        passenger2 = newUser("Passager 2");
    }

    private long newUser(String name) {
        User u = new User();
        u.setName(name);
        u.setEmail(name.replace(" ", "").toLowerCase() + "@mail.tn");
        u.setCity("Sfax");
        return users.create(u).getId();
    }

    /** Trajet Sfax -> Tunis dans 2 heures (coordonnees de Sfax). */
    private Ride newRide(int seats, String origin, String destination, double lat, double lon) {
        Ride r = new Ride();
        r.setDriverId(driver);
        r.setOrigin(origin);
        r.setDestination(destination);
        r.setDepartureAt(Instant.now().getEpochSecond() + 7200);
        r.setSeats(seats);
        r.setPrice(15);
        return rides.create(r, lat, lon);
    }

    private Ride sfaxTunis(int seats) {
        return newRide(seats, "sfax", "tunis", 34.7406, 10.76);
    }

    @Test
    void creerPuisLire() {
        Ride created = sfaxTunis(3);
        Ride found = rides.find(created.getId());
        assertEquals("sfax", found.getOrigin());
        assertEquals(3, found.getSeatsLeft());
        assertEquals("OPEN", found.getStatus());
        assertTrue(found.getPassengers().isEmpty());
    }

    @Test
    void reserverRetireUnePlace() {
        Ride r = sfaxTunis(3);
        assertEquals(2, rides.book(r.getId(), passenger1));
        Ride after = rides.find(r.getId());
        assertEquals(2, after.getSeatsLeft());
        assertEquals(List.of(passenger1), after.getPassengers());
    }

    @Test
    void doubleReservationRefusee() {
        Ride r = sfaxTunis(3);
        rides.book(r.getId(), passenger1);
        assertThrows(Conflict.class, () -> rides.book(r.getId(), passenger1));
        assertEquals(2, rides.find(r.getId()).getSeatsLeft());     // pas de place perdue
    }

    @Test
    void trajetCompletRefuse() {
        Ride r = sfaxTunis(1);
        rides.book(r.getId(), passenger1);
        assertThrows(Conflict.class, () -> rides.book(r.getId(), passenger2));
        assertEquals(0, rides.find(r.getId()).getSeatsLeft());     // jamais negatif
    }

    @Test
    void annulerRendLaPlace() {
        Ride r = sfaxTunis(2);
        rides.book(r.getId(), passenger1);
        assertTrue(rides.cancelBooking(r.getId(), passenger1));
        assertEquals(2, rides.find(r.getId()).getSeatsLeft());
        assertFalse(rides.cancelBooking(r.getId(), passenger1));   // deja annulee
    }

    @Test
    void trajetFermeRefuseLesReservations() {
        Ride r = sfaxTunis(2);
        rides.update(r.getId(), Map.of("status", "CLOSED"));
        assertThrows(Conflict.class, () -> rides.book(r.getId(), passenger1));
    }

    @Test
    void reduireLesPlacesSousLesReservationsEstUnConflit() {
        Ride r = sfaxTunis(3);
        rides.book(r.getId(), passenger1);
        rides.book(r.getId(), passenger2);
        assertThrows(Conflict.class, () -> rides.update(r.getId(), Map.of("seats", "1")));
        Ride ok = rides.update(r.getId(), Map.of("seats", "4"));
        assertEquals(2, ok.getSeatsLeft());                        // 4 places - 2 reservations
    }

    @Test
    void modifierUnTrajetInconnu() {
        assertNull(rides.update(9999, Map.of("price", "10")));
    }

    @Test
    void rechercheParVilles() {
        sfaxTunis(3);
        newRide(2, "sousse", "tunis", 35.8256, 10.6411);
        assertEquals(1, rides.search(null, null, 50, "sfax", "tunis").size());
        assertEquals(2, rides.search(null, null, 50, null, "tunis").size());
        assertEquals(0, rides.search(null, null, 50, "gabes", null).size());
        assertEquals(2, rides.search(null, null, 50, null, null).size());
    }

    @Test
    void rechercheAutourDUnPoint() {
        sfaxTunis(3);                                              // a Sfax
        newRide(2, "tunis", "sfax", 36.8065, 10.1815);             // a Tunis, ~235 km de Sfax
        List<Ride> near = rides.search(34.74, 10.76, 50, null, null);
        assertEquals(1, near.size());
        assertEquals("sfax", near.get(0).getOrigin());
        assertTrue(near.get(0).getDistanceKm() < 5);
    }

    @Test
    void supprimerRetireLeTrajetEtSesIndex() {
        Ride r = sfaxTunis(3);
        assertTrue(rides.delete(r.getId()));
        assertNull(rides.find(r.getId()));
        assertEquals(0, rides.search(null, null, 50, "sfax", null).size());
        assertEquals(0, rides.search(34.74, 10.76, 50, null, null).size());
        assertFalse(rides.delete(r.getId()));
    }
}
