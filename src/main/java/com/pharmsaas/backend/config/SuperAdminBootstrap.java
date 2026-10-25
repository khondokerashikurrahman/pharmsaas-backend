package com.pharmsaas.backend.config;


import com.pharmsaas.backend.modules.user.entity.SystemRole;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:}")
    private String adminEmail;

    @Value("${app.super-admin.password:}")
    private String adminPassword;

    @Value("${app.super-admin.name:System Super Admin}")
    private String adminName;

    public SuperAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // Do not create an administrator unless credentials
        // have explicitly been provided through configuration.
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            System.out.println(
                    "Super Admin bootstrap skipped: credentials not configured."
            );
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        if (adminPassword.length() < 12
                || adminPassword.getBytes(
                java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Super Admin password must be at least 12 characters "
                            + "and at most 72 UTF-8 bytes."
            );
        }

        var existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {

            User user = existingUser.get();

            if (user.getSystemRole() != SystemRole.SUPER_ADMIN) {
                throw new IllegalStateException(
                        "Configured Super Admin email already belongs "
                                + "to a non-admin account. No role was changed."
                );
            }

            System.out.println(
                    "Configured Super Admin account already exists."
            );
            return;
        }

        User admin = new User();
        admin.setFullName(
                adminName == null || adminName.isBlank()
                        ? "System Super Admin"
                        : adminName.trim()
        );
        admin.setEmail(email);
        admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
        );
        admin.setEnabled(true);
        admin.setSystemRole(SystemRole.SUPER_ADMIN);
        admin.setTenant(null);

        userRepository.save(admin);

        System.out.println("Super Admin account created successfully.");
    }
}

