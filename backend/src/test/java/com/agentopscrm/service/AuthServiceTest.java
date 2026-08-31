package com.agentopscrm.service;

import com.agentopscrm.dto.auth.LoginRequest;
import com.agentopscrm.dto.auth.SignupRequest;
import com.agentopscrm.entity.AppUser;
import com.agentopscrm.exception.DuplicateEmailException;
import com.agentopscrm.exception.InvalidCredentialsException;
import com.agentopscrm.repository.AppUserRepository;
import com.agentopscrm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService("unit-test-secret", 3600000);
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(), jwtService, true);
    }

    @Test
    void signupHashesPasswordAndDoesNotStorePlaintext() {
        when(userRepository.existsByEmailIgnoreCase("new@agentcrm.app")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        SignupRequest request = new SignupRequest();
        request.setFullName("Ada Lovelace");
        request.setEmail("new@agentcrm.app");
        request.setPassword("Secure#456");

        authService.signup(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser stored = captor.getValue();
        assertFalse(stored.getPasswordHash().contains("Secure#456"));
        assertTrue(stored.getPasswordHash().startsWith("$2"));
        assertEquals("new@agentcrm.app", stored.getEmail());
    }

    @Test
    void signupRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("demo@agentcrm.app")).thenReturn(true);
        SignupRequest request = new SignupRequest();
        request.setFullName("Demo");
        request.setEmail("demo@agentcrm.app");
        request.setPassword("Demo@1234");
        assertThrows(DuplicateEmailException.class, () -> authService.signup(request));
    }

    @Test
    void loginRejectsWrongPassword() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("demo@agentcrm.app");
        user.setFullName("Demo User");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("Demo@123"));
        when(userRepository.findByEmailIgnoreCase("demo@agentcrm.app")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("demo@agentcrm.app");
        request.setPassword("wrong-password");
        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
