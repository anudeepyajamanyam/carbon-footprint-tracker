package com.carbon.tracker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter.
 * - Auth endpoints (/api/auth/**): max 15 attempts per IP per minute
 * - Other API endpoints: max 120 requests per IP per minute
 *
 * No external dependencies needed — uses ConcurrentHashMap + timestamps.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_LIMIT = 15;
    private static final int API_LIMIT = 120;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, RequestBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only rate-limit API paths
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        boolean isAuthPath = path.startsWith("/api/auth/");
        int limit = isAuthPath ? AUTH_LIMIT : API_LIMIT;

        String bucketKey = clientIp + ":" + (isAuthPath ? "auth" : "api");
        RequestBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new RequestBucket());

        if (!bucket.tryAcquire(limit, WINDOW_MS)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Too many requests. Please wait before trying again.\",\"retryAfter\":60}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Thread-safe sliding window counter per key. */
    private static class RequestBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        synchronized boolean tryAcquire(int limit, long windowMs) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMs) {
                // Reset window
                windowStart = now;
                count.set(0);
            }
            if (count.incrementAndGet() > limit) {
                count.decrementAndGet();
                return false;
            }
            return true;
        }
    }
}
