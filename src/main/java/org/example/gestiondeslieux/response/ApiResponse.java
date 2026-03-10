package org.example.gestiondeslieux.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    @Schema(example = "Opération réussie")
    private String message;
    private T data;

    public ApiResponse(String message) {
        this.message = message;
    }
}
