package org.example.gestiondeslieux.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "collections", uniqueConstraints = {
        @UniqueConstraint(name = "uk_collection_user_tag_filter", columnNames = {"user_id", "tag_filter"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "tag_filter", length = 100)
    private String tagFilter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
