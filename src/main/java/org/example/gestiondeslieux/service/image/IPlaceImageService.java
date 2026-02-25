package org.example.gestiondeslieux.service.image;

import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.model.PlaceImage;
import org.springframework.web.multipart.MultipartFile;

public interface IPlaceImageService {
    PlaceImage uploadImage(MultipartFile file, Long placeId, Long userId);
    void deleteImage(Long imageId, Long userId);
    Place deleteMainImage(Long placeId, Long userId);
    byte[] getImageBytes(Long imageId, Long userId, String accessToken);
    String getImageContentType(Long imageId, Long userId, String accessToken);
}
