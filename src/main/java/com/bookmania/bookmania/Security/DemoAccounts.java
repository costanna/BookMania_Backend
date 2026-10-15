package com.bookmania.bookmania.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The two public demo accounts offered on the login screen (the portfolio
 * "try it" buttons): a reader who can use the app for real, and an admin
 * who can browse the whole admin panel but change nothing. Their
 * credentials are public by design, so anything that would let a visitor
 * lock others out (changing the password) or damage the catalog (admin
 * writes) is blocked for them - see DemoReadOnlyInterceptor and
 * AuthService.changePassword.
 */
@Component
public class DemoAccounts {

    private final boolean enabled;
    private final String readerEmail;
    private final String adminEmail;
    private final String password;

    public DemoAccounts(
            @Value("${app.demo.enabled:true}") boolean enabled,
            @Value("${app.demo.reader-email}") String readerEmail,
            @Value("${app.demo.admin-email}") String adminEmail,
            @Value("${app.demo.password}") String password) {
        this.enabled = enabled;
        this.readerEmail = readerEmail;
        this.adminEmail = adminEmail;
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getReaderEmail() {
        return readerEmail;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDemo(String email) {
        return enabled && (readerEmail.equalsIgnoreCase(email) || adminEmail.equalsIgnoreCase(email));
    }

    public boolean isDemoAdmin(String email) {
        return enabled && adminEmail.equalsIgnoreCase(email);
    }
}
