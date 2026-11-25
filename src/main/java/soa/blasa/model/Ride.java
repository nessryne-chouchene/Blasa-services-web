package soa.blasa.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Un trajet propose par un conducteur. Stocke dans Redis : HASH ride:{id}.
 * Les champs absents (null) ne sont pas envoyes dans le JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ride {

    private Long id;
    private Long driverId;
    private String origin;
    private String destination;
    private Long departureAt;      // date de depart en secondes (epoch)
    private Integer seats;         // places proposees
    private Integer seatsLeft;     // places restantes
    private Integer price;         // prix par place, en dinars
    private String status;         // OPEN ou CLOSED

    // Uniquement a l'entree (creation) : position de depart (sinon deduite de la ville)
    private Double originLat;
    private Double originLon;

    // Uniquement a la sortie
    private Double distanceKm;     // recherche "autour de moi"
    private List<Long> passengers; // detail d'un trajet

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Long getDepartureAt() { return departureAt; }
    public void setDepartureAt(Long departureAt) { this.departureAt = departureAt; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }

    public Integer getSeatsLeft() { return seatsLeft; }
    public void setSeatsLeft(Integer seatsLeft) { this.seatsLeft = seatsLeft; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOriginLat() { return originLat; }
    public void setOriginLat(Double originLat) { this.originLat = originLat; }

    public Double getOriginLon() { return originLon; }
    public void setOriginLon(Double originLon) { this.originLon = originLon; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public List<Long> getPassengers() { return passengers; }
    public void setPassengers(List<Long> passengers) { this.passengers = passengers; }
}
