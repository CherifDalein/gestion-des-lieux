package org.example.gestiondeslieux.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiscoverResponse {
    private String serverUrl;
    private String tokenLabel;
    private List<CollectionDto> collections;
    private List<PlaceDto> places;
    private Boolean locationAvailable;
}
