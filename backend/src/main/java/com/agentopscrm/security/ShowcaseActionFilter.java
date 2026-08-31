package com.agentopscrm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Optional kill switch for paid or irreversible external actions.
 * Off by default so the product can approve, search, call, and delete.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ShowcaseActionFilter extends OncePerRequestFilter {

    public static final String MESSAGE = "External actions are currently disabled.";

    private final boolean disabled;

    public ShowcaseActionFilter(
            @Value("${app.showcase.external-actions-disabled:false}") boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (disabled && isBlocked(request.getMethod(), request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":\"SHOWCASE_ACTION_DISABLED\",\"message\":\"" + MESSAGE + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean isBlocked(String method, String uri) {
        if (uri == null) {
            return false;
        }
        if ("POST".equalsIgnoreCase(method) && uri.matches(".*/leads/[^/]+/voice-calls/start")) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && uri.matches(".*/lead-finder/runs/?$")) {
            return true;
        }
        if ("PUT".equalsIgnoreCase(method) && uri.matches(".*/approvals/[^/]+/approve")) {
            return true;
        }
        return "DELETE".equalsIgnoreCase(method) && uri.matches(".*/businesses/[^/]+/?$");
    }
}
