package org.example.gestiondeslieux.service.export;

import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.model.Place;

import java.util.List;

public interface IExportService {
    String exportToGpx(List<Place> places, String collectionName);
    String exportToKml(List<Place> places, String collectionName);
    String exportToGeoJson(List<Place> places, String collectionName);
    List<Place> importFromGpx(String gpxContent, Long userId);
    List<Place> importFromKml(String kmlContent, Long userId);
    List<Place> importFromGeoJson(String geoJsonContent, Long userId);
    ExportFormat detectFormat(String content);
}
