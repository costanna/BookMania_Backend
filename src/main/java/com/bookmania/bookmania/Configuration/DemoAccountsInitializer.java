package com.bookmania.bookmania.Configuration;

import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.Role;
import com.bookmania.bookmania.Repository.UserRepository;
import com.bookmania.bookmania.Security.DemoAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Makes sure both demo accounts exist on every startup, so the login
 * screen's demo buttons always work without seeding the database by hand.
 * It also puts them back in a usable state - right role, active, the public
 * password and no pending penalty - since the service restarts every time
 * Railway wakes it up, that doubles as a periodic reset.
 */
@Component
@RequiredArgsConstructor
public class DemoAccountsInitializer implements ApplicationRunner {

    private final DemoAccounts demoAccounts;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!demoAccounts.isEnabled()) {
            return;
        }
        ensureAccount(demoAccounts.getReaderEmail(), "Lector Demo", Role.USER);
        ensureAccount(demoAccounts.getAdminEmail(), "Admin Demo", Role.ADMIN);
    }

    private void ensureAccount(String email, String name, Role role) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> User.builder().email(email).name(name).build());

        user.setRole(role);
        user.setActive(true);
        user.setPenaltyUntil(null);
        // Only re-hash when it doesn't already match: BCrypt is slow on
        // purpose, and this runs on every cold start.
        if (user.getPassword() == null || !passwordEncoder.matches(demoAccounts.getPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(demoAccounts.getPassword()));
        }
        userRepository.save(user);
    }
}
