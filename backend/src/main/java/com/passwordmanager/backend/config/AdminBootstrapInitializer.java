package com.passwordmanager.backend.config;

import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.UserRepository;
import com.passwordmanager.backend.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the first SUPER_ADMIN user when the database has no users
 * and ADMIN_INIT_* environment variables are configured.
 */
@Component
public class AdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${app.admin.init.username:}")
    private String initUsername;

    @Value("${app.admin.init.email:}")
    private String initEmail;

    @Value("${app.admin.init.password:}")
    private String initPassword;

    public AdminBootstrapInitializer(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        if (isBlank(initUsername) || isBlank(initEmail) || isBlank(initPassword)) {
            log.warn("No users exist in database. Configure ADMIN_INIT_USERNAME, ADMIN_INIT_EMAIL, and ADMIN_INIT_PASSWORD to initialize the first SUPER_ADMIN account.");
            return;
        }

        if (initPassword.length() < 8) {
            log.error("ADMIN_INIT_PASSWORD must be at least 8 characters. Initial admin bootstrap skipped.");
            return;
        }

        User admin = new User(
                initUsername.trim(),
                initEmail.trim(),
                passwordEncoder.encode(initPassword),
                "SUPER_ADMIN"
        );
        User saved = userRepository.save(admin);
        auditService.log(saved.getId(), saved.getUsername(), "BOOTSTRAP_ADMIN", "USER", saved.getId(), null, "Initialized initial SUPER_ADMIN account");
        log.info("Initialized initial SUPER_ADMIN account '{}'", saved.getUsername());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
