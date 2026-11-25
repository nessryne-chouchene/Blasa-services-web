package soa.blasa.repository;

/** Regle metier non respectee (trajet complet, deja reserve...) -> la ressource repond 409. */
public class Conflict extends RuntimeException {

    public Conflict(String message) {
        super(message);
    }
}
