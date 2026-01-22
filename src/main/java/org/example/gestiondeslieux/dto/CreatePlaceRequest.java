package org.example.gestiondeslieux.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

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

    // getters & setters
}