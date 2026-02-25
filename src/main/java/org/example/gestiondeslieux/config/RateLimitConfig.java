package org.example.gestiondeslieux.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    public static class RateLimitFilter extends OncePerRequestFilter {

        // key -> [tokenCount, lastRefillTimestamp]
        private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

        private long getCapacity(String uri) {
            return uri.contains("/api/auth/login") ? 10L : 100L;
        }

        private synchronized boolean tryConsume(String key, long capacity) {
            long now = System.currentTimeMillis();
            long[] state = buckets.computeIfAbsent(key, k -> new long[]{capacity, now});
            long elapsed = now - state[1];
            // refill at rate capacity/minute
            long refilled = (long) (elapsed / 60000.0 * capacity);
            if (refilled > 0) {
                state[0] = Math.min(capacity, state[0] + refilled);
                state[1] = now;
            }
            if (state[0] > 0) {
                state[0]--;
                return true;
            }
            return false;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain)
                throws ServletException, IOException {
            String uri = request.getRequestURI();
            String key = request.getRemoteAddr() + ":" + uri;
            long capacity = getCapacity(uri);

            if (tryConsume(key, capacity)) {
                chain.doFilter(request, response);
            } else {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Trop de requêtes, réessayez plus tard\"}");
            }
        }
    }
}
