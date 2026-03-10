package org.example.gestiondeslieux.util;

import org.springframework.http.MediaType;

public class GeoMediaTypes {

    public static final String GPX_VALUE     = "application/gpx+xml";
    public static final String KML_VALUE     = "application/vnd.google-earth.kml+xml";
    public static final String GEOJSON_VALUE = "application/geo+json";

    public static final MediaType GPX     = MediaType.valueOf(GPX_VALUE);
    public static final MediaType KML     = MediaType.valueOf(KML_VALUE);
    public static final MediaType GEOJSON = MediaType.valueOf(GEOJSON_VALUE);

    private GeoMediaTypes() {}
}
