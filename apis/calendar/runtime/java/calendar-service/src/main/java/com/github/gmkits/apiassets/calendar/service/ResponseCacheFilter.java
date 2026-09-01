package com.github.gmkits.apiassets.calendar.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 为确定性查询设置统一缓存策略；错误与运维端点不缓存。 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public final class ResponseCacheFilter extends OncePerRequestFilter {
    private final boolean protectedApi;

    public ResponseCacheFilter(CalendarProperties properties) {
        this.protectedApi = properties.getUpstreamToken() != null
                && !properties.getUpstreamToken().isBlank();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        int status = response.getStatus();
        if (!((status >= 200 && status < 300) || status == 304)
                || response.containsHeader("Cache-Control")
                || !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) return;
        String path = request.getRequestURI();
        String visibility = protectedApi ? "private" : "public";
        if (protectedApi) response.setHeader("Vary", "Authorization");
        if (path.startsWith("/v1/calendar/assets/") && !path.endsWith("/manifest")) {
            response.setHeader("Cache-Control", visibility + ",max-age=86400");
        } else if (path.endsWith("/metadata") || path.endsWith("/assets/manifest")) {
            response.setHeader("Cache-Control", visibility + ",max-age=60");
        } else if (path.startsWith("/v1/calendar/")) {
            response.setHeader("Cache-Control", visibility + ",max-age=3600");
        }
    }
}
