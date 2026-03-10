package org.example.gestiondeslieux.service.export;

import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.model.Place;

import java.util.List;

public interface IExportService {
    String exportToGpx(List<Place> places, String collectionName);
    String exportToKml(List<Place> places, String collectionName);
    String exportToGeoJson(List<Place> places, String collectionName);
    List<Place> importFromGpx(String gpxContent);
    List<Place> importFromKml(String kmlContent);
    List<Place> importFromGeoJson(String geoJsonContent);
    ExportFormat detectFormat(String content);
}
