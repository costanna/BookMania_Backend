package com.bookmania.bookmania.Security;

import com.bookmania.bookmania.Exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoReadOnlyInterceptorTest {

    private final DemoAccounts demoAccounts =
            new DemoAccounts(true, "lector@bookmania.dev", "admin@bookmania.dev", "demo1234");
    private final DemoReadOnlyInterceptor interceptor = new DemoReadOnlyInterceptor(demoAccounts);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private boolean call(String method) {
        return interceptor.preHandle(new MockHttpServletRequest(method, "/api/books/1"),
                new MockHttpServletResponse(), new Object());
    }

    @Test
    void demoAdmin_canRead() {
        authenticateAs("admin@bookmania.dev");
        assertThat(call("GET")).isTrue();
    }

    @Test
    void demoAdmin_cannotWrite() {
        authenticateAs("admin@bookmania.dev");
        for (String method : List.of("POST", "PUT", "DELETE", "PATCH")) {
            assertThatThrownBy(() -> call(method))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Modo demo");
        }
    }

    @Test
    void demoReader_canStillWrite() {
        authenticateAs("lector@bookmania.dev");
        assertThat(call("POST")).isTrue();
    }

    @Test
    void realAdmin_canWrite() {
        authenticateAs("anna@bookmania.com");
        assertThat(call("DELETE")).isTrue();
    }

    @Test
    void anonymousRequest_isNotBlocked() {
        assertThat(call("POST")).isTrue(); // e.g. the login request itself
    }

    @Test
    void disabledDemo_blocksNothing() {
        DemoReadOnlyInterceptor off = new DemoReadOnlyInterceptor(
                new DemoAccounts(false, "lector@bookmania.dev", "admin@bookmania.dev", "demo1234"));
        authenticateAs("admin@bookmania.dev");
        assertThat(off.preHandle(new MockHttpServletRequest("DELETE", "/api/books/1"),
                new MockHttpServletResponse(), new Object())).isTrue();
    }
}
