package com.example.backend.config.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter ghi log chi tiết cho mỗi HTTP request vào/ra của ứng dụng:
 * - Phương thức HTTP, URI, query parameters.
 * - Client IP.
 * - Mã phản hồi HTTP (status code) và thời gian thực thi (latency ms).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Bỏ qua log verbose cho các tài nguyên tĩnh swagger/webjars
        if (isStaticOrDocAsset(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String clientIp = getClientIp(request);

        log.info("[HTTP IN] {} {}{} from {}", method, uri, queryString, clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("[HTTP OUT] {} {}{} -> {} (took {} ms)", method, uri, queryString, status, durationMs);
            } else if (status >= 400) {
                log.warn("[HTTP OUT] {} {}{} -> {} (took {} ms)", method, uri, queryString, status, durationMs);
            } else {
                log.info("[HTTP OUT] {} {}{} -> {} (took {} ms)", method, uri, queryString, status, durationMs);
            }
        }
    }

    private boolean isStaticOrDocAsset(String uri) {
        return uri.startsWith("/swagger-ui/") || uri.startsWith("/v3/api-docs/") || uri.endsWith(".ico")
                || uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
