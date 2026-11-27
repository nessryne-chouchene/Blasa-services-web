package soa.blasa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Un utilisateur Blasa (conducteur et/ou passager). Stocke dans Redis : HASH user:{id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private Long id;
    private String name;
    private String email;
    private String city;
    private int ratingSum;
    private int ratingCount;

    // Uniquement a l'entree (jamais renvoye) : stocke sous forme de hash PBKDF2 dans Redis
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getRatingSum() { return ratingSum; }
    public void setRatingSum(int ratingSum) { this.ratingSum = ratingSum; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    /** Note moyenne calculee (0 si aucune note). */
    public double getAverageRating() {
        return ratingCount == 0 ? 0.0 : Math.round(100.0 * ratingSum / ratingCount) / 100.0;
    }
}
