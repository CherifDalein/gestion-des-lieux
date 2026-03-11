package org.example.gestiondeslieux.service.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gestiondeslieux.enums.ExportFormat;
import org.example.gestiondeslieux.exceptions.InvalidFormatException;
import org.example.gestiondeslieux.model.Place;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService implements IExportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            if (p.getCreatedAt() != null) {
                sb.append(",\"createdAt\":").append(jsonString(p.getCreatedAt().toString()));
            }
            if (p.getUpdatedAt() != null) {
                sb.append(",\"updatedAt\":").append(jsonString(p.getUpdatedAt().toString()));
            }
            sb.append("}}");
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public List<Place> importFromGpx(String gpxContent) {
        Document doc = parseXml(gpxContent, "GPX");
        List<Element> waypoints = selectElements(doc, "//*[local-name()='wpt']", "GPX");
        List<Place> places = new ArrayList<>();
        for (Element wpt : waypoints) {
            Double lat = parseDouble(wpt.getAttribute("lat"));
            Double lon = parseDouble(wpt.getAttribute("lon"));
            if (lat == null || lon == null) {
                continue;
            }

            String title = defaultIfBlank(selectString(wpt, "./*[local-name()='name']/text()", "GPX"), "Imported");
            String description = emptyToNull(selectString(wpt, "./*[local-name()='desc']/text()", "GPX"));
            String tagsRaw = selectString(wpt, ".//*[local-name()='tags']/text()", "GPX");

            Place place = Place.builder()
                    .title(title)
                    .description(description)
                    .latitude(lat)
                    .longitude(lon)
                    .tags(splitCsvTags(tagsRaw))
                    .build();
            places.add(place);
        }
        return places;
    }

    @Override
    public List<Place> importFromKml(String kmlContent) {
        Document doc = parseXml(kmlContent, "KML");
        List<Element> placemarks = selectElements(doc, "//*[local-name()='Placemark']", "KML");
        List<Place> places = new ArrayList<>();

        for (Element placemark : placemarks) {
            String coordinatesRaw = selectString(placemark, ".//*[local-name()='coordinates']/text()", "KML");
            double[] lonLat = parseLonLat(coordinatesRaw);
            if (lonLat == null) {
                continue;
            }

            String title = defaultIfBlank(selectString(placemark, "./*[local-name()='name']/text()", "KML"), "Imported");
            String description = emptyToNull(selectString(placemark, "./*[local-name()='description']/text()", "KML"));
            String tagsRaw = selectString(
                    placemark,
                    ".//*[local-name()='Data' and @name='tags']/*[local-name()='value']/text()",
                    "KML"
            );
            String imageUrl = emptyToNull(selectString(
                    placemark,
                    ".//*[local-name()='Data' and @name='imageUrl']/*[local-name()='value']/text()",
                    "KML"
            ));

            Place place = Place.builder()
                    .title(title)
                    .description(description)
                    .latitude(lonLat[1])
                    .longitude(lonLat[0])
                    .imageUrl(imageUrl)
                    .tags(splitCsvTags(tagsRaw))
                    .build();
            places.add(place);
        }

        return places;
    }

    @Override
    public List<Place> importFromGeoJson(String geoJsonContent) {
        JsonNode root = parseJson(geoJsonContent, "GEOJSON");
        List<JsonNode> features = extractGeoJsonFeatures(root);
        List<Place> places = new ArrayList<>();

        for (JsonNode feature : features) {
            JsonNode geometry = feature.path("geometry");
            if (!"Point".equalsIgnoreCase(geometry.path("type").asText())) {
                continue;
            }
            JsonNode coordinates = geometry.path("coordinates");
            if (!coordinates.isArray() || coordinates.size() < 2) {
                continue;
            }

            Double lon = parseDoubleNode(coordinates.get(0));
            Double lat = parseDoubleNode(coordinates.get(1));
            if (lat == null || lon == null) {
                continue;
            }

            JsonNode properties = feature.path("properties");
            String title = firstNonBlank(
                    properties.path("title").asText(null),
                    properties.path("name").asText(null),
                    "Imported"
            );
            String description = emptyToNull(properties.path("description").asText(null));
            String imageUrl = emptyToNull(properties.path("imageUrl").asText(null));
            List<String> tags = extractGeoJsonTags(properties.path("tags"));

            Place place = Place.builder()
                    .title(title)
                    .description(description)
                    .latitude(lat)
                    .longitude(lon)
                    .imageUrl(imageUrl)
                    .tags(tags)
                    .build();
            places.add(place);
        }

        return places;
    }

    @Override
    public ExportFormat detectFormat(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidFormatException("null", "contenu vide");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("<")) {
            if (trimmed.contains("<gpx")) return ExportFormat.GPX;
            if (trimmed.contains("<kml")) return ExportFormat.KML;
        }
        if (trimmed.startsWith("{")) {
            JsonNode root = parseJson(trimmed, "GEOJSON");
            String type = root.path("type").asText(null);
            if ("FeatureCollection".equalsIgnoreCase(type) || "Feature".equalsIgnoreCase(type)) {
                return ExportFormat.GEOJSON;
            }
        }
        throw new InvalidFormatException("inconnu", "impossible de detecter le format GPX/KML/GeoJSON");
    }

    private Document parseXml(String xml, String format) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception ex) {
            throw new InvalidFormatException(format, "contenu XML invalide");
        }
    }

    private JsonNode parseJson(String json, String format) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new InvalidFormatException(format, "contenu JSON invalide");
        }
    }

    private List<Element> selectElements(Node context, String expression, String format) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(expression, context, XPathConstants.NODESET);
            List<Element> elements = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element) {
                    elements.add(element);
                }
            }
            return elements;
        } catch (XPathExpressionException ex) {
            throw new InvalidFormatException(format, "structure XML non exploitable");
        }
    }

    private String selectString(Node context, String expression, String format) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String value = (String) xpath.evaluate(expression, context, XPathConstants.STRING);
            return value != null ? value.trim() : null;
        } catch (XPathExpressionException ex) {
            throw new InvalidFormatException(format, "structure XML non exploitable");
        }
    }

    private List<JsonNode> extractGeoJsonFeatures(JsonNode root) {
        String type = root.path("type").asText("");
        List<JsonNode> features = new ArrayList<>();
        if ("FeatureCollection".equalsIgnoreCase(type)) {
            JsonNode featuresNode = root.path("features");
            if (!featuresNode.isArray()) {
                throw new InvalidFormatException("GEOJSON", "FeatureCollection sans tableau 'features'");
            }
            featuresNode.forEach(features::add);
            return features;
        }
        if ("Feature".equalsIgnoreCase(type)) {
            features.add(root);
            return features;
        }
        throw new InvalidFormatException("GEOJSON", "type GeoJSON non supporté (attendu : FeatureCollection ou Feature)");
    }

    private List<String> extractGeoJsonTags(JsonNode tagsNode) {
        if (tagsNode == null || tagsNode.isMissingNode() || tagsNode.isNull()) {
            return new ArrayList<>();
        }
        if (tagsNode.isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode tagNode : tagsNode) {
                String tag = emptyToNull(tagNode.asText(null));
                if (tag != null) {
                    tags.add(tag);
                }
            }
            return tags;
        }
        if (tagsNode.isTextual()) {
            return splitCsvTags(tagsNode.asText());
        }
        return new ArrayList<>();
    }

    private List<String> splitCsvTags(String tagsRaw) {
        List<String> tags = new ArrayList<>();
        if (tagsRaw == null || tagsRaw.isBlank()) {
            return tags;
        }
        for (String token : tagsRaw.split(",")) {
            String normalized = emptyToNull(token);
            if (normalized != null) {
                tags.add(normalized);
            }
        }
        return tags;
    }

    private double[] parseLonLat(String coordinatesRaw) {
        if (coordinatesRaw == null || coordinatesRaw.isBlank()) {
            return null;
        }
        String[] tuples = coordinatesRaw.trim().split("\\s+");
        for (String tuple : tuples) {
            if (tuple.isBlank()) {
                continue;
            }
            String[] parts = tuple.split(",");
            if (parts.length < 2) {
                continue;
            }
            Double lon = parseDouble(parts[0]);
            Double lat = parseDouble(parts[1]);
            if (lon != null && lat != null) {
                return new double[]{lon, lat};
            }
        }
        return null;
    }

    private Double parseDoubleNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        return parseDouble(node.asText(null));
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return fallback;
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
}
