package org.example.gestiondeslieux.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class ImportPlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceService placeService; // pour la logique existante
    private final ObjectMapper objectMapper;

    public ImportPlaceService(PlaceRepository placeRepository, PlaceService placeService, ObjectMapper objectMapper) {
        this.placeRepository = placeRepository;
        this.placeService = placeService;
        this.objectMapper = objectMapper;
    }

    /** Parse un fichier GPX/KML/GeoJSON et retourne la liste de lieux détectés */
    public List<Place> parsePlacesFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename().toLowerCase();
        InputStream is = file.getInputStream();

        if (filename.endsWith(".geojson") || filename.endsWith(".json")) {
            return parseGeoJson(is);
        } else if (filename.endsWith(".gpx")) {
            return parseGpx(is);
        } else if (filename.endsWith(".kml")) {
            return parseKml(is);
        } else {
            throw new IllegalArgumentException("Format de fichier non supporté");
        }
    }

    private List<Place> parseGeoJson(InputStream is) throws Exception {
        List<Place> places = new ArrayList<>();
        JsonNode root = objectMapper.readTree(is);
        JsonNode features = root.path("features");
        if (features.isArray()) {
            for (JsonNode f : features) {
                JsonNode coords = f.path("geometry").path("coordinates");
                JsonNode props = f.path("properties");
                if (coords.isArray() && coords.size() >= 2) {
                    Place p = new Place();
                    p.setLongitude(coords.get(0).asDouble());
                    p.setLatitude(coords.get(1).asDouble());
                    p.setTitle(props.path("name").asText(null));
                    p.setDescription(props.path("description").asText(null));
                    places.add(p);
                }
            }
        }
        return places;
    }

    private List<Place> parseGpx(InputStream is) throws Exception {
        List<Place> places = new ArrayList<>();
        Scanner scanner = new Scanner(is);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("<wpt")) {
                Place p = new Place();
                String latStr = line.split("lat=\"")[1].split("\"")[0];
                String lonStr = line.split("lon=\"")[1].split("\"")[0];
                p.setLatitude(Double.parseDouble(latStr));
                p.setLongitude(Double.parseDouble(lonStr));
                if (scanner.hasNextLine()) {
                    String nameLine = scanner.nextLine();
                    if (nameLine.contains("<name>")) {
                        String name = nameLine.split("<name>")[1].split("</name>")[0];
                        p.setTitle(name);
                    }
                }
                places.add(p);
            }
        }
        return places;
    }

    private List<Place> parseKml(InputStream is) throws Exception {
        List<Place> places = new ArrayList<>();
        Scanner scanner = new Scanner(is);
        Place current = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("<Placemark>")) {
                current = new Place();
            } else if (line.startsWith("<name>") && current != null) {
                current.setTitle(line.replace("<name>", "").replace("</name>", ""));
            } else if (line.startsWith("<coordinates>") && current != null) {
                String coords = line.replace("<coordinates>", "").replace("</coordinates>", "");
                String[] parts = coords.split(",");
                current.setLongitude(Double.parseDouble(parts[0]));
                current.setLatitude(Double.parseDouble(parts[1]));
                places.add(current);
                current = null;
            }
        }
        return places;
    }

    /** Vérifie si un lieu existe déjà par latitude/longitude */
    public boolean existsByCoordinates(Double latitude, Double longitude) {
        return placeRepository.existsByLatitudeAndLongitude(latitude, longitude);
    }

    /** Récupère un lieu existant par lat/lon */
    public Optional<Place> findByCoordinates(Double latitude, Double longitude) {
        return placeRepository.findByLatitudeAndLongitude(latitude, longitude);
    }

    /** Sauvegarde un lieu pour un utilisateur */
    public Place savePlace(Place place, Long userId) {
        // Crée un CreatePlaceRequest avec le constructeur complet
        org.example.gestiondeslieux.dto.CreatePlaceRequest request =
                new org.example.gestiondeslieux.dto.CreatePlaceRequest(
                        place.getTitle(),
                        place.getDescription(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getTags()
                );

        // Appelle le service PlaceService pour gérer la création (validation, tags, userId)
        return placeService.createPlace(request, userId);
    }


    /** Met à jour les collections (placeholder) */
    public void updateCollections() {
        // TODO: logiques réelles des collections par tags
        System.out.println("Collections mises à jour");
    }
}
