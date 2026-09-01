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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** 拒绝契约未声明的 query 参数，避免拼写错误被静默忽略。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class StrictQueryParameterFilter extends OncePerRequestFilter {
    private static final Pattern DAY = Pattern.compile("/v1/calendar/dates/\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("/v1/calendar/months/\\d{1,4}/\\d{1,2}");
    private static final Pattern YEAR = Pattern.compile("/v1/calendar/years/\\d{1,4}");
    private static final Pattern SOLAR_TERMS = Pattern.compile("/v1/calendar/solar-terms/\\d{1,4}");
    private static final Pattern HOLIDAY_ASSET = Pattern.compile(
            "/v1/calendar/assets/holidays/[A-Z]{2}(?:-[A-Z0-9]{1,8})*/\\d{1,4}\\.hday");
    private static final Set<String> NONE = Collections.emptySet();
    private static final Set<String> DAY_PARAMS = set("region", "locale", "fields");
    private static final Set<String> RANGE_PARAMS = set("from", "to", "region", "locale", "fields");
    private static final Set<String> MONTH_YEAR_PARAMS = set("region", "locale", "fields");
    private static final Set<String> WORKDAY_PARAMS = set("from", "to", "region");
    private static final Set<String> HOLIDAY_PARAMS = set("year", "region", "locale");
    private static final Set<String> NEXT_PARAMS = set("from", "region", "locale");
    private static final Set<String> LUNAR_PARAMS = set("date");
    private static final Set<String> SOLAR_PARAMS = set("year", "month", "day", "leapMonth");

    private final ObjectMapper mapper;

    public StrictQueryParameterFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Set<String> allowed = allowed(request.getMethod(), request.getRequestURI());
        if (allowed != null) {
            for (String parameter : request.getParameterMap().keySet()) {
                if (!allowed.contains(parameter)) {
                    reject(parameter, request, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(String parameter, HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        ApiProblem problem = new ApiProblem(
                "urn:api-assets:calendar:error:invalid-argument", "Bad Request", 400,
                "不支持的 query 参数: " + parameter, request.getRequestURI(),
                "INVALID_ARGUMENT", requestId);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }

    private static Set<String> allowed(String method, String path) {
        if ("POST".equals(method) && "/v1/calendar/dates:batch".equals(path)) return NONE;
        if (!"GET".equals(method) && !"HEAD".equals(method)) return null;
        if (DAY.matcher(path).matches()) return DAY_PARAMS;
        if ("/v1/calendar/dates".equals(path)) return RANGE_PARAMS;
        if (MONTH.matcher(path).matches() || YEAR.matcher(path).matches()) return MONTH_YEAR_PARAMS;
        if ("/v1/calendar/workdays/count".equals(path)) return WORKDAY_PARAMS;
        if ("/v1/calendar/holidays".equals(path)) return HOLIDAY_PARAMS;
        if ("/v1/calendar/holidays/next".equals(path)) return NEXT_PARAMS;
        if ("/v1/calendar/lunar/from-solar".equals(path)) return LUNAR_PARAMS;
        if ("/v1/calendar/solar/from-lunar".equals(path)) return SOLAR_PARAMS;
        if (SOLAR_TERMS.matcher(path).matches() || HOLIDAY_ASSET.matcher(path).matches()) return NONE;
        if (path.startsWith("/v1/calendar/") || path.startsWith("/internal/")) return NONE;
        return null;
    }

    private static Set<String> set(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
