package org.example.gestiondeslieux.service.export;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.exceptions.InvalidFormatException;
import org.example.gestiondeslieux.model.Place;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ExportService implements IExportService {

    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    public String exportToGpx(List<Place> places, String collectionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\" creator=\"Lieux-Manager\"\n");
        sb.append("     xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        sb.append("  <metadata>\n");
        sb.append("    <name>").append(escapeXml(collectionName)).append("</name>\n");
        sb.append("    <time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .append("</time>\n");
        sb.append("  </metadata>\n");
        for (Place p : places) {
            sb.append("  <wpt lat=\"").append(p.getLatitude())
              .append("\" lon=\"").append(p.getLongitude()).append("\">\n");
            sb.append("    <name>").append(escapeXml(p.getTitle())).append("</name>\n");
            if (p.getDescription() != null) {
                sb.append("    <desc>").append(escapeXml(p.getDescription())).append("</desc>\n");
            }
            if (p.getCreatedAt() != null) {
                sb.append("    <time>").append(p.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                  .append("</time>\n");
            }
            if (!p.getTags().isEmpty()) {
                sb.append("    <extensions>\n");
                sb.append("      <tags>").append(String.join(",", p.getTags())).append("</tags>\n");
                sb.append("    </extensions>\n");
            }
            sb.append("  </wpt>\n");
        }
        sb.append("</gpx>");
        return sb.toString();
    }

    @Override
    public String exportToKml(List<Place> places, String collectionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("  <Document>\n");
        sb.append("    <name>").append(escapeXml(collectionName)).append("</name>\n");
        for (Place p : places) {
            sb.append("    <Placemark>\n");
            sb.append("      <name>").append(escapeXml(p.getTitle())).append("</name>\n");
            String desc = (p.getDescription() != null ? p.getDescription() : "")
                    + (p.getTags().isEmpty() ? "" : "<br/>Tags: " + String.join(", ", p.getTags()));
            sb.append("      <description><![CDATA[").append(desc).append("]]></description>\n");
            sb.append("      <Point><coordinates>")
              .append(p.getLongitude()).append(",").append(p.getLatitude()).append(",0")
              .append("</coordinates></Point>\n");
            sb.append("      <ExtendedData>\n");
            sb.append("        <Data name=\"tags\"><value>")
              .append(String.join(",", p.getTags())).append("</value></Data>\n");
            sb.append("        <Data name=\"id\"><value>").append(p.getId()).append("</value></Data>\n");
            sb.append("      </ExtendedData>\n");
            sb.append("    </Placemark>\n");
        }
        sb.append("  </Document>\n");
        sb.append("</kml>");
        return sb.toString();
    }

    @Override
    public String exportToGeoJson(List<Place> places, String collectionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"FeatureCollection\",\"name\":")
          .append(jsonString(collectionName))
          .append(",\"features\":[");
        for (int i = 0; i < places.size(); i++) {
            Place p = places.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"type\":\"Feature\",\"id\":\"").append(p.getId()).append("\"");
            sb.append(",\"geometry\":{\"type\":\"Point\",\"coordinates\":[")
              .append(p.getLongitude()).append(",").append(p.getLatitude()).append("]}");
            sb.append(",\"properties\":{");
            sb.append("\"title\":").append(jsonString(p.getTitle()));
            sb.append(",\"description\":").append(jsonString(p.getDescription()));
            sb.append(",\"imageUrl\":").append(jsonString(p.getImageUrl()));
            sb.append(",\"tags\":[");
            List<String> tags = p.getTags();
            for (int j = 0; j < tags.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append(jsonString(tags.get(j)));
            }
            sb.append("]");
            if (p.getCreatedAt() != null)
                sb.append(",\"createdAt\":").append(jsonString(p.getCreatedAt().toString()));
            if (p.getUpdatedAt() != null)
                sb.append(",\"updatedAt\":").append(jsonString(p.getUpdatedAt().toString()));
            sb.append("}}");
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    @Transactional
    public List<Place> importFromGpx(String gpxContent, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        List<Place> result = new ArrayList<>();
        Pattern wptPattern = Pattern.compile(
                "<wpt\\s+lat=\"([^\"]+)\"\\s+lon=\"([^\"]+)\"[^>]*>(.*?)</wpt>",
                Pattern.DOTALL);
        Matcher wptMatcher = wptPattern.matcher(gpxContent);
        while (wptMatcher.find()) {
            double lat = Double.parseDouble(wptMatcher.group(1));
            double lon = Double.parseDouble(wptMatcher.group(2));
            String wptBody = wptMatcher.group(3);
            String title = extractXmlTag(wptBody, "name", "Imported");
            String desc = extractXmlTag(wptBody, "desc", null);
            List<String> tags = new ArrayList<>();
            String tagsStr = extractXmlTag(wptBody, "tags", null);
            if (tagsStr != null && !tagsStr.isBlank()) {
                for (String t : tagsStr.split(",")) tags.add(t.trim());
            }
            Place place = Place.builder()
                    .title(title).description(desc)
                    .latitude(lat).longitude(lon)
                    .tags(tags).user(user).build();
            result.add(placeRepository.save(place));
        }
        return result;
    }

    @Override
    @Transactional
    public List<Place> importFromKml(String kmlContent, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        List<Place> result = new ArrayList<>();
        Pattern pmPattern = Pattern.compile("<Placemark>(.*?)</Placemark>", Pattern.DOTALL);
        Matcher pmMatcher = pmPattern.matcher(kmlContent);
        while (pmMatcher.find()) {
            String pmBody = pmMatcher.group(1);
            String title = extractXmlTag(pmBody, "name", "Imported");
            Pattern coordPattern = Pattern.compile("<coordinates>([^<]+)</coordinates>");
            Matcher coordMatcher = coordPattern.matcher(pmBody);
            if (!coordMatcher.find()) continue;
            String[] coords = coordMatcher.group(1).trim().split(",");
            double lon = Double.parseDouble(coords[0]);
            double lat = Double.parseDouble(coords[1]);
            List<String> tags = new ArrayList<>();
            Pattern tagsPattern = Pattern.compile("<Data name=\"tags\"><value>([^<]*)</value>");
            Matcher tagsMatcher = tagsPattern.matcher(pmBody);
            if (tagsMatcher.find()) {
                for (String t : tagsMatcher.group(1).split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isBlank()) tags.add(trimmed);
                }
            }
            Place place = Place.builder()
                    .title(title).latitude(lat).longitude(lon)
                    .tags(tags).user(user).build();
            result.add(placeRepository.save(place));
        }
        return result;
    }

    @Override
    @Transactional
    public List<Place> importFromGeoJson(String geoJsonContent, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        List<Place> result = new ArrayList<>();
        Pattern featPattern = Pattern.compile("\\{\"type\":\"Feature\".*?(?=,\\{\"type\":\"Feature\"|\\]\\})", Pattern.DOTALL);
        Matcher featMatcher = featPattern.matcher(geoJsonContent);
        while (featMatcher.find()) {
            String feat = featMatcher.group();
            Pattern coordPattern = Pattern.compile("\"coordinates\":\\[([\\d.\\-]+),([\\d.\\-]+)");
            Matcher coordMatcher = coordPattern.matcher(feat);
            if (!coordMatcher.find()) continue;
            double lon = Double.parseDouble(coordMatcher.group(1));
            double lat = Double.parseDouble(coordMatcher.group(2));
            Pattern titlePattern = Pattern.compile("\"title\":\"([^\"]+)\"");
            Matcher titleMatcher = titlePattern.matcher(feat);
            String title = titleMatcher.find() ? titleMatcher.group(1) : "Imported";
            Place place = Place.builder()
                    .title(title).latitude(lat).longitude(lon)
                    .tags(new ArrayList<>()).user(user).build();
            result.add(placeRepository.save(place));
        }
        return result;
    }

    @Override
    public ExportFormat detectFormat(String content) {
        if (content == null) throw new InvalidFormatException("null", "contenu vide");
        String trimmed = content.trim();
        if (trimmed.contains("<gpx")) return ExportFormat.GPX;
        if (trimmed.contains("<kml")) return ExportFormat.KML;
        if (trimmed.startsWith("{") && trimmed.contains("FeatureCollection")) return ExportFormat.GEOJSON;
        throw new InvalidFormatException("inconnu", "impossible de détecter le format GPX/KML/GeoJSON");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private String extractXmlTag(String xml, String tag, String defaultVal) {
        Pattern p = Pattern.compile("<" + tag + ">([^<]*)</" + tag + ">", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : defaultVal;
    }
}
