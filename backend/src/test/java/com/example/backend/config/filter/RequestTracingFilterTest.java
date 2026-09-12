package com.example.backend.config.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class RequestTracingFilterTest {

    private final RequestTracingFilter filter = new RequestTracingFilter();

    @Test
    void doFilter_WhenCustomHeaderProvided_PropagatesToResponseAndClearsMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "custom-trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcValueDuringFilter = new String[1];
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcValueDuringFilter[0] = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY);
            }
        };

        filter.doFilter(request, response, chain);

        assertEquals("custom-trace-123", response.getHeader("X-Request-Id"));
        assertEquals("custom-trace-123", mdcValueDuringFilter[0]);
        assertNull(MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY), "MDC must be cleared after filter completion");
    }

    @Test
    void doFilter_WhenNoHeaderProvided_GeneratesTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcValueDuringFilter = new String[1];
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcValueDuringFilter[0] = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY);
            }
        };

        filter.doFilter(request, response, chain);

        String responseTraceId = response.getHeader("X-Request-Id");
        assertNotNull(responseTraceId);
        assertTrue(responseTraceId.startsWith("req-"));
        assertEquals(responseTraceId, mdcValueDuringFilter[0]);
        assertNull(MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY));
    }
}
