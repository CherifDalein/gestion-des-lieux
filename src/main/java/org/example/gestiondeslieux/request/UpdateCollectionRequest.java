package org.example.gestiondeslieux.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCollectionRequest {

    @Size(max = 200)
    private String name;

    private String tagFilter;
}
