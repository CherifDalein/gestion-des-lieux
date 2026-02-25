package org.example.gestiondeslieux.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public class HttpCacheUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private HttpCacheUtils() {}

    public static String computeEtag(Object body) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(body);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(json);
            return '"' + HexFormat.of().formatHex(digest) + '"';
        } catch (Exception e) {
            return '"' + String.valueOf(body.hashCode()) + '"';
        }
    }

    public static boolean isNotModified(HttpServletRequest request,
                                        String etag,
                                        LocalDateTime lastModified) {
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return true;
        }
        String ifModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && lastModified != null) {
            try {
                long sinceEpoch = java.time.ZonedDateTime
                        .parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant()
                        .getEpochSecond();
                long lastModifiedEpoch = lastModified.toEpochSecond(ZoneOffset.UTC);
                if (lastModifiedEpoch <= sinceEpoch) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static <T> ResponseEntity<T> buildCachedResponse(T body,
                                                             LocalDateTime lastModified,
                                                             HttpServletRequest request,
                                                             String cacheControl) {
        String etag = computeEtag(body);
        if (isNotModified(request, etag, lastModified)) {
            return ResponseEntity.status(304).build();
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl);
        if (lastModified != null) {
            builder = builder.header(HttpHeaders.LAST_MODIFIED,
                    lastModified.atZone(ZoneOffset.UTC)
                               .format(DateTimeFormatter.RFC_1123_DATE_TIME));
        }
        return builder.body(body);
    }
}
