package br.com.amcash.auth.service;

import br.com.amcash.auth.dto.response.GoogleLoginResponse;
import br.com.amcash.config.JwtService;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    private GoogleTokenService googleTokenService;
    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        googleTokenService = mock(GoogleTokenService.class);
        userRepository = mock(UserRepository.class);
        JwtService jwtService = new JwtService(
                "amcash-test-secret-key-with-at-least-32-characters",
                86_400_000L
        );
        authService = new AuthService(googleTokenService, userRepository, jwtService);
    }

    @Test
    void shouldCreateUserOnFirstGoogleLogin() {
        GoogleIdentity identity = new GoogleIdentity(
                "google-subject-123",
                "usuario@gmail.com",
                "Usuário AMCash",
                "https://example.com/avatar.jpg"
        );
        UUID userId = UUID.randomUUID();

        when(googleTokenService.verify("google-id-token")).thenReturn(identity);
        when(userRepository.findByGoogleSubject(identity.subject())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        GoogleLoginResponse response = authService.loginWithGoogle("google-id-token");

        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86_400L, response.expiresIn());
        assertEquals(userId, response.user().id());
        assertEquals(identity.email(), response.user().email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldReuseUserOnFollowingGoogleLogin() {
        GoogleIdentity identity = new GoogleIdentity(
                "google-subject-123",
                "novo-email@gmail.com",
                "Nome atualizado",
                null
        );
        User existingUser = new User(
                identity.subject(),
                "email-antigo@gmail.com",
                "Nome antigo",
                null
        );
        existingUser.setId(UUID.randomUUID());

        when(googleTokenService.verify("google-id-token")).thenReturn(identity);
        when(userRepository.findByGoogleSubject(identity.subject()))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        GoogleLoginResponse response = authService.loginWithGoogle("google-id-token");

        assertEquals(existingUser.getId(), response.user().id());
        assertEquals("novo-email@gmail.com", response.user().email());
        assertEquals("Nome atualizado", response.user().name());
    }
}
