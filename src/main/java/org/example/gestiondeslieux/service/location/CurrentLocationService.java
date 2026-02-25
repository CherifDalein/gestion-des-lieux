package org.example.gestiondeslieux.service.location;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.controller.api.LocationApiController;
import org.example.gestiondeslieux.dto.CurrentLocationDto;
import org.example.gestiondeslieux.dto.PublicLocationDto;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.model.CurrentLocation;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.CurrentLocationRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.UpdateLocationRequest;
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@RequiredArgsConstructor
public class CurrentLocationService implements ICurrentLocationService {

    private final CurrentLocationRepository currentLocationRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

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

    @Override
    @Transactional(readOnly = true)
    public CurrentLocationDto convertToDto(CurrentLocation location) {
        Hibernate.initialize(location.getUser());
        CurrentLocationDto dto = modelMapper.map(location, CurrentLocationDto.class);
        try {
            dto.add(linkTo(methodOn(LocationApiController.class).getCurrent(null)).withSelfRel());
            dto.add(linkTo(methodOn(LocationApiController.class).update(null, null)).withRel("update"));
            dto.add(linkTo(methodOn(LocationApiController.class).deleteCurrent(null)).withRel("delete"));
            dto.add(linkTo(methodOn(LocationApiController.class).startSharing(null)).withRel("share"));
            dto.add(linkTo(methodOn(LocationApiController.class).stopSharing(null)).withRel("unshare"));
        } catch (Exception ignored) {
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicLocationDto convertToPublicDto(CurrentLocation location) {
        PublicLocationDto dto = modelMapper.map(location, PublicLocationDto.class);
        dto.setAgeSeconds(location.getUpdatedAt() != null
                ? ChronoUnit.SECONDS.between(location.getUpdatedAt(), LocalDateTime.now())
                : null);
        try {
            if (location.getShareToken() != null) {
                dto.add(linkTo(methodOn(LocationApiController.class)
                        .getPublic(location.getShareToken()))
                        .withSelfRel());
            }
        } catch (Exception ignored) {
        }
        return dto;
    }
}
