package com.bookmania.bookmania;

import com.bookmania.bookmania.Dtos.AuthRequest;
import com.bookmania.bookmania.Dtos.AuthResponse;
import com.bookmania.bookmania.Dtos.ChangePasswordRequest;
import com.bookmania.bookmania.Dtos.RegisterRequest;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.Role;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.UserRepository;
import com.bookmania.bookmania.Security.CurrentUserService;
import com.bookmania.bookmania.Security.JwtUtil;
import com.bookmania.bookmania.Services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserDetails userDetails;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).name("Anna").email("anna@test.com").password("hashed")
                .role(Role.USER).active(true).build();
    }

    @Test
    void register_newEmail_savesUserAndReturnsToken() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Anna").email("anna@test.com").password("plain").build();

        when(userRepository.findByEmail("anna@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain")).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("anna@test.com")).thenReturn(userDetails);
        lenient().when(userDetails.getUsername()).thenReturn("anna@test.com");
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("anna@test.com");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("anna@test.com")
                        && u.getPassword().equals("hashed")
                        && u.getRole() == Role.USER
                        && u.isActive()
        ));
    }

    @Test
    void register_duplicateEmail_throwsBusinessException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Anna").email("anna@test.com").password("plain").build();

        when(userRepository.findByEmail("anna@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_authenticatesAndReturnsToken() {
        AuthRequest request = AuthRequest.builder().email("anna@test.com").password("plain").build();

        when(userDetailsService.loadUserByUsername("anna@test.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmail("anna@test.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getName()).isEqualTo("Anna");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_userVanishedAfterAuthentication_throwsResourceNotFoundException() {
        // Extremely unlikely (deleted between authenticate() succeeding and
        // this re-fetch) but should surface as a clear 404-ish error, not an
        // unexplained NPE further down.
        AuthRequest request = AuthRequest.builder().email("anna@test.com").password("plain").build();

        when(userDetailsService.loadUserByUsername("anna@test.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token");
        when(userRepository.findByEmail("anna@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_correctCurrentPassword_encodesAndSavesNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPlain");
        request.setNewPassword("newPlain");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("oldPlain", "hashed")).thenReturn(true);
        when(passwordEncoder.matches("newPlain", "hashed")).thenReturn(false);
        when(passwordEncoder.encode("newPlain")).thenReturn("newHashed");

        authService.changePassword(request);

        verify(userRepository).save(argThat(u -> u.getPassword().equals("newHashed")));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBusinessException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("newPlain");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("actual no es correcta");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_newPasswordSameAsCurrent_throwsBusinessException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("samePlain");
        request.setNewPassword("samePlain");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("samePlain", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("distinta de la actual");

        verify(userRepository, never()).save(any());
    }
}
