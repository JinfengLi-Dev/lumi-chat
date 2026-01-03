package com.lumichat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter for authenticating internal service-to-service calls.
 * The IM server uses this to call the API server for message persistence.
 */
@Slf4j
@Component
public class InternalServiceFilter extends OncePerRequestFilter {

    private static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String DEVICE_ID_HEADER = "X-Device-Id";

    @Value("${app.internal.allowed-services:im-server}")
    private String allowedServices;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Determine the request path for matching
        // In a real servlet container, getServletPath() excludes context path (e.g., "/api/v1")
        // In MockMvc tests, getServletPath() may be empty, so fall back to getRequestURI()
        // We also need to strip the context path from URI if present
        String requestPath = request.getServletPath();
        if (requestPath == null || requestPath.isEmpty()) {
            // MockMvc or similar test environment - use requestURI but strip context path
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
                requestPath = uri.substring(contextPath.length());
            } else {
                requestPath = uri;
            }
        }

        log.debug("InternalServiceFilter: checking path={}", requestPath);

        // Only process internal service endpoints
        if (!isInternalServicePath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String serviceName = request.getHeader(INTERNAL_SERVICE_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);
        String deviceId = request.getHeader(DEVICE_ID_HEADER);

        // Validate service header
        if (serviceName == null || !isAllowedService(serviceName)) {
            log.warn("Unauthorized internal service access attempt from: {}", serviceName);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"code\": 403, \"message\": \"Unauthorized service\"}");
            return;
        }

        // Create authentication with internal service principal
        InternalServicePrincipal principal = new InternalServicePrincipal(
                serviceName,
                userId != null ? Long.parseLong(userId) : null,
                deviceId
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Internal service authenticated: {} for user {}", serviceName, userId);

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedService(String serviceName) {
        String[] services = allowedServices.split(",");
        for (String service : services) {
            if (service.trim().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the request path is an internal service path that requires
     * X-Internal-Service authentication.
     */
    private boolean isInternalServicePath(String requestPath) {
        // Internal API endpoints
        if (requestPath.startsWith("/internal/")) {
            return true;
        }
        // Sync queue endpoint (called by IM server for offline message queueing)
        if (requestPath.equals("/sync/queue")) {
            return true;
        }
        return false;
    }

    /**
     * Principal for internal service calls.
     */
    public record InternalServicePrincipal(
            String serviceName,
            Long userId,
            String deviceId
    ) {}
}
