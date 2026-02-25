package org.example.gestiondeslieux.service.image;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.exceptions.UnauthorizedAccessException;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.model.PlaceImage;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.PlaceImageRepository;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.UserRepository;
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
        if (!image.getUploadedBy().getId().equals(userId)) {
            throw new UnauthorizedAccessException("supprimer cette image");
        }
        try {
            Files.deleteIfExists(Paths.get(image.getFilePath()));
        } catch (IOException ignored) {}
        placeImageRepository.delete(image);
    }

    @Override
    public byte[] getImageBytes(Long imageId) {
        PlaceImage image = placeImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceImage", "id", imageId));
        try {
            return Files.readAllBytes(Paths.get(image.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture image", e);
        }
    }

    @Override
    public String getImageContentType(Long imageId) {
        return placeImageRepository.findById(imageId)
                .map(PlaceImage::getContentType)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceImage", "id", imageId));
    }
}
