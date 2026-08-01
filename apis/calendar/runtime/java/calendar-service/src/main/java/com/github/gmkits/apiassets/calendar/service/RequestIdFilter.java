package com.github.gmkits.apiassets.calendar.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** 统一生成请求标识并记录结构化的请求完成日志。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    public static final String HEADER = "X-Request-Id";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Logger LOG = LoggerFactory.getLogger(RequestIdFilter.class);
    private final String dataVersion;

    public RequestIdFilter(ValidatedAssetStore assets) {
        this.dataVersion = assets.dataVersion();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && VALID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();
        long started = System.nanoTime();
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        MDC.put("requestId", requestId);
        MDC.put("dataVersion", dataVersion);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMicros = (System.nanoTime() - started) / 1_000;
            LOG.debug("request method={} path={} status={} latencyMicros={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMicros);
            MDC.remove("requestId");
            MDC.remove("dataVersion");
        }
    }
}
