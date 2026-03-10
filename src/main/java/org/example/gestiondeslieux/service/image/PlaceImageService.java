package org.example.gestiondeslieux.service.image;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.enums.Permission;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.exceptions.UnauthorizedAccessException;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.model.PlaceImage;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.PlaceImageRepository;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.service.token.IAccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaceImageService implements IPlaceImageService {

    private final PlaceImageRepository placeImageRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final IAccessTokenService accessTokenService;

    @Value("${app.upload.dir:./uploads/images}")
    private String uploadDir;

    @Override
    @Transactional
    public PlaceImage uploadImage(MultipartFile file, Long placeId, Long userId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
        if (!place.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("uploader une image pour ce lieu");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        try {
            deleteCurrentMainImage(place);

            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            PlaceImage image = PlaceImage.builder()
                    .fileName(fileName)
                    .contentType(file.getContentType())
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .place(place)
                    .uploadedBy(user)
                    .build();
            PlaceImage saved = placeImageRepository.save(image);
            place.setImageUrl("/api/images/" + saved.getId());
            placeRepository.save(place);
            return saved;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload de l'image", e);
        }
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId, Long userId) {
        PlaceImage image = placeImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceImage", "id", imageId));
        if (!image.getPlace().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("supprimer cette image");
        }
        clearPlaceImageIfMain(image);
        deleteStoredImage(image);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getImageBytes(Long imageId, Long userId, String accessToken) {
        PlaceImage image = getAuthorizedImage(imageId, userId, accessToken);
        try {
            return Files.readAllBytes(Paths.get(image.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture image", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getImageContentType(Long imageId, Long userId, String accessToken) {
        return getAuthorizedImage(imageId, userId, accessToken).getContentType();
    }

    @Override
    @Transactional
    public Place deleteMainImage(Long placeId, Long userId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "id", placeId));
        if (!place.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("supprimer l'image principale de ce lieu");
        }

        deleteCurrentMainImage(place);
        place.setImageUrl(null);
        return placeRepository.save(place);
    }

    private PlaceImage getAuthorizedImage(Long imageId, Long userId, String accessToken) {
        PlaceImage image = placeImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceImage", "id", imageId));

        Long ownerId = image.getPlace().getUser().getId();
        if (userId != null && ownerId.equals(userId)) {
            return image;
        }
        Long placeId = image.getPlace().getId();
        if (accessToken != null
                && accessTokenService.hasPermission(accessToken, ResourceType.PLACE, placeId, Permission.READ)) {
            return image;
        }
        throw new ResourceNotFoundException("PlaceImage", "id", imageId);
    }

    private void deleteCurrentMainImage(Place place) {
        Long currentImageId = extractImageId(place.getImageUrl());
        if (currentImageId == null) {
            return;
        }
        placeImageRepository.findById(currentImageId)
                .filter(img -> img.getPlace() != null && img.getPlace().getId().equals(place.getId()))
                .ifPresent(this::deleteStoredImage);
    }

    private void clearPlaceImageIfMain(PlaceImage image) {
        Place place = image.getPlace();
        if (place == null || place.getImageUrl() == null) {
            return;
        }
        Long mainImageId = extractImageId(place.getImageUrl());
        if (mainImageId != null && mainImageId.equals(image.getId())) {
            place.setImageUrl(null);
            placeRepository.save(place);
        }
    }

    private void deleteStoredImage(PlaceImage image) {
        try {
            Files.deleteIfExists(Paths.get(image.getFilePath()));
        } catch (IOException ignored) {
        }
        placeImageRepository.delete(image);
    }

    private Long extractImageId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        int idx = imageUrl.lastIndexOf('/');
        if (idx < 0 || idx == imageUrl.length() - 1) {
            return null;
        }
        try {
            return Long.parseLong(imageUrl.substring(idx + 1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
