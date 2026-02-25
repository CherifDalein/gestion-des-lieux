package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlaceDto {
    private Long id;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;
    private int collectionCount;
}
