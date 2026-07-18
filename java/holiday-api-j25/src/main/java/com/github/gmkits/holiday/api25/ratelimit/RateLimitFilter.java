package com.github.gmkits.holiday.api25.ratelimit;

import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import com.github.gmkits.holiday.api25.config.RequestIdFilter;
import com.github.gmkits.holiday.api25.dto.ApiErrorResponse;
import com.github.gmkits.holiday.api25.dto.ApiResponses;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于令牌桶算法的简易限流器，保护小机器不被高并发打崩。
 *
 * <p>按客户端 IP 限流，支持全局 QPS 上限和单 IP QPS 上限。
 * IP 桶由有容量和过期策略的 Caffeine 缓存管理，令牌消费使用 CAS，
 * 避免长时间运行时被不同来源 IP 撑大内存。</p>
 *
 * <p>当限流触发时返回 HTTP 429 和统一错误格式。</p>
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {

    private final HolidayApi25Properties properties;
    private final ObjectMapper objectMapper;

    /** 全局令牌桶 */
    private final TokenBucket globalBucket = new TokenBucket();

    /** 按 IP 的令牌桶 */
    private final Cache<String, TokenBucket> ipBuckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || !properties.getRateLimit().isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);

        // 全局限流检查
        if (!globalBucket.tryConsume(properties.getRateLimit().getGlobalQps())) {
            writeRateLimitResponse(response, request, "RATE_LIMITED", "全局请求过于频繁，请稍后重试");
            return;
        }

        // 单 IP 限流检查
        TokenBucket ipBucket = ipBuckets.get(clientIp, k -> new TokenBucket());
        if (!ipBucket.tryConsume(properties.getRateLimit().getPerIpQps())) {
            writeRateLimitResponse(response, request, "IP_RATE_LIMITED",
                    "当前 IP 请求过于频繁，请稍后重试");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response, HttpServletRequest request,
                                        String code, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "1");

        ApiErrorResponse errorResponse = ApiResponses.error(code, message, null, request);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 基于滑动窗口的简易令牌桶。
     *
     * <p>每秒补充 {@code qps} 个令牌，最多积累 {@code qps * 2} 个令牌（允许小幅突发）。
     * 使用 CAS 操作保证线程安全，无锁设计适合虚拟线程。</p>
     */
    static class TokenBucket {
        private final AtomicLong lastRefillNanos = new AtomicLong(System.nanoTime());
        private final AtomicLong tokens = new AtomicLong(0);
        private volatile boolean initialized = false;

        boolean tryConsume(int qps) {
            if (qps <= 0) {
                return true; // 限流关闭
            }
            long maxTokens = qps * 2L;

            // 首次调用时初始化令牌数为 maxTokens，避免冷启动被限流
            if (!initialized) {
                tokens.compareAndSet(0, maxTokens);
                initialized = true;
            }

            long now = System.nanoTime();
            long last = lastRefillNanos.get();
            long elapsed = now - last;

            // 补充令牌（CAS 更新保证线程安全）
            if (elapsed > 0) {
                long newTokens = elapsed * qps / 1_000_000_000L;
                if (newTokens > 0 && lastRefillNanos.compareAndSet(last, now)) {
                    while (true) {
                        long current = tokens.get();
                        long updated = Math.min(current + newTokens, maxTokens);
                        if (tokens.compareAndSet(current, updated)) {
                            break;
                        }
                    }
                }
            }

            // 尝试消费一个令牌
            while (true) {
                long current = tokens.get();
                if (current <= 0) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }
    }
}
