package com.github.gmkits.apiassets.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 可选的内部 Bearer Token；公网身份治理仍由前置平台负责。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public final class UpstreamTokenFilter extends OncePerRequestFilter {
    private final byte[] expected;
    private final ObjectMapper mapper;

    public UpstreamTokenFilter(CalendarProperties properties, ObjectMapper mapper) {
        String token = properties.getUpstreamToken();
        this.expected = token == null || token.isBlank()
                ? null : token.getBytes(StandardCharsets.UTF_8);
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (expected == null) return true;
        String path = request.getRequestURI();
        return !(path.startsWith("/v1/calendar/") || path.equals("/v1/calendar")
                || path.equals("/internal/metrics"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        byte[] actual = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        if (MessageDigest.isEqual(expected, actual)) {
            chain.doFilter(request, response);
            return;
        }
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        ApiProblem problem = new ApiProblem(
                "urn:api-assets:calendar:error:upstream-unauthorized",
                "Unauthorized", 401, "缺少或提供了无效的上游访问令牌",
                request.getRequestURI(), "UPSTREAM_UNAUTHORIZED", requestId);
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
