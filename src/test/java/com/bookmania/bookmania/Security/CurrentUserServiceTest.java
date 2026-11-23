package com.bookmania.bookmania.Security;

import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String... roles) {
        var authorities = List.of(roles).stream().map(SimpleGrantedAuthority::new).toList();
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(email).password("x").authorities(authorities).build();
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, authorities);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getCurrentUser_existingUser_returnsUser() {
        authenticateAs("user@test.com", "ROLE_USER");
        User user = User.builder().id(1L).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getCurrentUser_missingUser_throwsResourceNotFoundException() {
        authenticateAs("ghost@test.com", "ROLE_USER");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void isCurrentUserAdmin_withAdminRole_returnsTrue() {
        authenticateAs("admin@test.com", "ROLE_ADMIN");

        assertThat(currentUserService.isCurrentUserAdmin()).isTrue();
    }

    @Test
    void isCurrentUserAdmin_withUserRole_returnsFalse() {
        authenticateAs("user@test.com", "ROLE_USER");

        assertThat(currentUserService.isCurrentUserAdmin()).isFalse();
    }
}
