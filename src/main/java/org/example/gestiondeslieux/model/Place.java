package org.example.gestiondeslieux.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private  Double latitude;
    @Column(nullable = false)
    private Double longitude;


    @ElementCollection
    @CollectionTable(
            name = "place_tags",
            joinColumns = @JoinColumn(name = "place_id")
    )
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    private Instant createdAt;

    // Constructors
    public Place() {}

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    //Getters and Setters

}

