package org.example.gestiondeslieux.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "current_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double accuracy;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    @Column(length = 255)
    private String shareToken;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
