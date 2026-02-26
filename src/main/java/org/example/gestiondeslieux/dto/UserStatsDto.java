package org.example.gestiondeslieux.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private long collectionCount;
    private long ownedPlaceCount;
    private long sharedCollectionCount;
}
