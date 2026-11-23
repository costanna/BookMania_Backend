package com.bookmania.bookmania.Configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets the app be wired up with a single DATABASE_URL variable, the way Neon
 * (and most hosted Postgres providers) hand out their connection string.
 *
 * That string looks like
 * {@code postgresql://user:password@host:port/database?sslmode=require}, but Spring's JDBC
 * datasource needs a {@code jdbc:postgresql://host:port/database} URL plus
 * username/password as separate properties. This runs before the context is
 * refreshed and translates one into the other.
 *
 * Only kicks in when a DATABASE_URL property is actually present, so local
 * dev keeps using the SPRING_DATASOURCE_* properties/defaults in
 * application.properties untouched, and it takes precedence over them when
 * both are set — matching "just paste Neon's URL into DATABASE_URL" as the
 * simplest path to a working deploy.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl);

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                username = decode(parts[0]);
                password = parts.length > 1 ? decode(parts[1]) : null;
            }

            int port = uri.getPort() != -1 ? uri.getPort() : 5432;
            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(uri.getHost()).append(':').append(port).append(uri.getPath());
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl.append('?').append(uri.getQuery());
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl.toString());
            if (username != null) {
                props.put("spring.datasource.username", username);
            }
            if (password != null) {
                props.put("spring.datasource.password", password);
            }

            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
        } catch (Exception e) {
            // Malformed DATABASE_URL: fall back to whatever SPRING_DATASOURCE_*
            // properties/defaults are configured, and let the datasource
            // auto-configuration fail with its own (clearer) error if needed.
            System.err.println(
                    "DatabaseUrlEnvironmentPostProcessor: could not parse DATABASE_URL, ignoring it — " + e.getMessage());
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
