package com.bookmania.bookmania.Configuration;

import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.Role;
import com.bookmania.bookmania.Repository.UserRepository;
import com.bookmania.bookmania.Security.DemoAccounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoAccountsInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DemoAccountsInitializer initializer(boolean enabled) {
        DemoAccounts demoAccounts =
                new DemoAccounts(enabled, "lector@bookmania.dev", "admin@bookmania.dev", "demo1234");
        return new DemoAccountsInitializer(demoAccounts, userRepository, passwordEncoder);
    }

    @Test
    void missingAccounts_areCreatedWithTheirRoles() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("demo1234")).thenReturn("hashed");

        initializer(true).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(saved.capture());
        List<User> users = saved.getAllValues();
        assertThat(users).extracting(User::getEmail, User::getRole, User::getPassword).containsExactly(
                tuple("lector@bookmania.dev", Role.USER, "hashed"),
                tuple("admin@bookmania.dev", Role.ADMIN, "hashed"));
        assertThat(users).allMatch(User::isActive);
    }

    @Test
    void existingAccount_isResetToAUsableState() {
        User tampered = User.builder().email("lector@bookmania.dev").name("Lector Demo")
                .password("someone-elses-hash").role(Role.ADMIN).active(false)
                .penaltyUntil(LocalDate.now().plusDays(10)).build();
        when(userRepository.findByEmail("lector@bookmania.dev")).thenReturn(Optional.of(tampered));
        when(userRepository.findByEmail("admin@bookmania.dev")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("demo1234", "someone-elses-hash")).thenReturn(false);
        when(passwordEncoder.encode("demo1234")).thenReturn("hashed");

        initializer(true).run(null);

        assertThat(tampered.getRole()).isEqualTo(Role.USER);
        assertThat(tampered.isActive()).isTrue();
        assertThat(tampered.getPenaltyUntil()).isNull();
        assertThat(tampered.getPassword()).isEqualTo("hashed");
    }

    @Test
    void disabled_touchesNothing() {
        initializer(false).run(null);
        verify(userRepository, never()).save(any());
    }
}
