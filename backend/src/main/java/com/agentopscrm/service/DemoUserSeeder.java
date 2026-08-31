package com.agentopscrm.service;

import com.agentopscrm.entity.AppUser;
import com.agentopscrm.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DemoUserSeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@agentcrm.app";
    public static final String DEMO_NAME = "Demo User";

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;
    private final String demoPassword;

    public DemoUserSeeder(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.seed-demo-user:true}") boolean seedEnabled,
            @Value("${app.auth.demo-password:Demo@123}") String demoPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        AppUser user = new AppUser();
        user.setFullName(DEMO_NAME);
        user.setEmail(DEMO_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Seeded sample access account {}", DEMO_EMAIL);
    }
}
