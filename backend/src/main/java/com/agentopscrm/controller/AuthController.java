package com.agentopscrm.controller;

import com.agentopscrm.dto.auth.AuthSessionResponse;
import com.agentopscrm.dto.auth.AuthUserResponse;
import com.agentopscrm.dto.auth.LoginRequest;
import com.agentopscrm.dto.auth.SignupRequest;
import com.agentopscrm.entity.AppUser;
import com.agentopscrm.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long expirationMs;

    public AuthController(
            AuthService authService,
            @Value("${app.auth.cookie-name:agentcrm_session}") String cookieName,
            @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
            @Value("${app.auth.cookie-same-site:Lax}") String cookieSameSite,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthSessionResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response) {
        AuthSessionResponse session = authService.signup(request);
        writeSessionCookie(response, session.getToken());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthSessionResponse session = authService.login(request);
        writeSessionCookie(response, session.getToken());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie expired = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
        return ResponseEntity.noContent().build();
    }

    private void writeSessionCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
