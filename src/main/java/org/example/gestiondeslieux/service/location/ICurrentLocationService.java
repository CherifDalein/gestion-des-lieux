package org.example.gestiondeslieux.service.location;

import org.example.gestiondeslieux.dto.CurrentLocationDto;
import org.example.gestiondeslieux.dto.PublicLocationDto;
import org.example.gestiondeslieux.model.CurrentLocation;
import org.example.gestiondeslieux.request.UpdateLocationRequest;

public interface ICurrentLocationService {
    CurrentLocation updateLocation(UpdateLocationRequest request, Long userId);
    CurrentLocation getCurrentLocation(Long userId);
    void deleteLocation(Long userId);
    CurrentLocation startSharing(Long userId);
    void stopSharing(Long userId);
    CurrentLocation getSharedLocation(String shareToken);
    CurrentLocationDto convertToDto(CurrentLocation location);
    PublicLocationDto convertToPublicDto(CurrentLocation location);
}
