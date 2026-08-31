package com.agentopscrm.service;

import com.agentopscrm.dto.auth.AuthSessionResponse;
import com.agentopscrm.dto.auth.AuthUserResponse;
import com.agentopscrm.dto.auth.LoginRequest;
import com.agentopscrm.dto.auth.SignupRequest;
import com.agentopscrm.entity.AppUser;
import com.agentopscrm.exception.DuplicateEmailException;
import com.agentopscrm.exception.InvalidCredentialsException;
import com.agentopscrm.repository.AppUserRepository;
import com.agentopscrm.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean externalActionsDisabled;

    public AuthService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.showcase.external-actions-disabled:false}") boolean externalActionsDisabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.externalActionsDisabled = externalActionsDisabled;
    }

    @Transactional
    public AuthSessionResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        AppUser user = new AppUser();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return toSession(user);
    }

    @Transactional(readOnly = true)
    public AuthSessionResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toSession(user);
    }

    public AuthUserResponse toUserResponse(AppUser user) {
        return new AuthUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                externalActionsDisabled,
                true);
    }

    private AuthSessionResponse toSession(AppUser user) {
        return new AuthSessionResponse(jwtService.createToken(user), toUserResponse(user));
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
