package com.bookmania.bookmania.Security;

import com.bookmania.bookmania.Exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Makes the demo admin read-only: it can open every admin screen (all GETs),
 * but any request that would change data is refused with a 403 before it
 * reaches a controller. The credentials are public, so without this any
 * visitor could delete or edit the production catalog.
 */
@Component
@RequiredArgsConstructor
public class DemoReadOnlyInterceptor implements HandlerInterceptor {

    private static final Set<String> READ_ONLY_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final DemoAccounts demoAccounts;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (READ_ONLY_METHODS.contains(request.getMethod())) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && demoAccounts.isDemoAdmin(authentication.getName())) {
            throw new ForbiddenException(
                    "Modo demo: el administrador de demostración puede consultarlo todo, pero no hacer cambios"
            );
        }
        return true;
    }
}
