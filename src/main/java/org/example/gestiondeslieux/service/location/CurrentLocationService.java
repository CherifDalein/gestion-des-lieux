package org.example.gestiondeslieux.service.location;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.model.CurrentLocation;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.CurrentLocationRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.UpdateLocationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentLocationService implements ICurrentLocationService {

    private final CurrentLocationRepository currentLocationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CurrentLocation updateLocation(UpdateLocationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        CurrentLocation location = currentLocationRepository.findByUserId(userId)
                .orElseGet(() -> CurrentLocation.builder().user(user).isShared(false).build());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAccuracy(request.getAccuracy());
        location.setTimestamp(LocalDateTime.now());
        return currentLocationRepository.save(location);
    }

    @Override
    public CurrentLocation getCurrentLocation(Long userId) {
        return currentLocationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CurrentLocation", "userId", userId));
    }

    @Override
    @Transactional
    public void deleteLocation(Long userId) {
        currentLocationRepository.findByUserId(userId)
                .ifPresent(currentLocationRepository::delete);
    }

    @Override
    @Transactional
    public CurrentLocation startSharing(Long userId) {
        CurrentLocation location = getCurrentLocation(userId);
        String shareToken = UUID.randomUUID().toString();
        location.setIsShared(true);
        location.setShareToken(shareToken);
        return currentLocationRepository.save(location);
    }

    @Override
    @Transactional
    public void stopSharing(Long userId) {
        currentLocationRepository.findByUserId(userId).ifPresent(loc -> {
            loc.setIsShared(false);
            loc.setShareToken(null);
            currentLocationRepository.save(loc);
        });
    }

    @Override
    public CurrentLocation getSharedLocation(String shareToken) {
        CurrentLocation location = currentLocationRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("CurrentLocation", "shareToken", shareToken));
        if (!location.getIsShared()) {
            throw new ResourceNotFoundException("CurrentLocation", "shareToken", shareToken);
        }
        return location;
    }
}
