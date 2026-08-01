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
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        int status = response.getStatus();
        if (!((status >= 200 && status < 300) || status == 304)
                || response.containsHeader("Cache-Control")) return;
        String path = request.getRequestURI();
        if (path.startsWith("/v1/calendar/assets/") && !path.endsWith("/manifest")) {
            response.setHeader("Cache-Control", "public,max-age=86400");
        } else if (path.endsWith("/metadata") || path.endsWith("/assets/manifest")) {
            response.setHeader("Cache-Control", "public,max-age=60");
        } else if (path.startsWith("/v1/calendar/")) {
            response.setHeader("Cache-Control", "public,max-age=3600");
        }
    }
}
