package org.example.gestiondeslieux.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class CreatePlaceRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String title;

    private String description;

    private Set<String> tags;

    // Constructeur vide généré par Lombok @Data (optionnel ici)
    public CreatePlaceRequest() {}

    // Constructeur complet à 5 paramètres
    public CreatePlaceRequest(String title, String description, Double latitude, Double longitude, Set<String> tags) {
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tags = tags;
    }
}
