package com.example.backend.config.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter đảm bảo mỗi request HTTP đều có một traceId duy nhất.
 * - Nhận header X-Request-Id hoặc X-Trace-Id từ client/gateway nếu có, hoặc tự sinh UUID.
 * - Đưa traceId vào SLF4J MDC để mọi dòng log trong luồng xử lý đều hiển thị traceId.
 * - Gán header X-Request-Id vào HTTP Response để client/frontend tiện đối soát khi gặp lỗi.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader(TRACE_ID_HEADER);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            response.setHeader(REQUEST_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
