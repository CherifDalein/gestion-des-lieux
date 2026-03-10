package org.example.gestiondeslieux.config;

import org.example.gestiondeslieux.util.GeoMediaTypes;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(true)
            .parameterName("format")
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json",    MediaType.APPLICATION_JSON)
            .mediaType("gpx",     GeoMediaTypes.GPX)
            .mediaType("kml",     GeoMediaTypes.KML)
            .mediaType("geojson", GeoMediaTypes.GEOJSON);
    }
}
