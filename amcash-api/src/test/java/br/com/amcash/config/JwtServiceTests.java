package br.com.amcash.config;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTests {

    private final JwtService jwtService = new JwtService(
            "amcash-test-secret-key-with-at-least-32-characters",
            86_400_000L
    );

    @Test
    void shouldGenerateAndReadAValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "usuario@amcash.com", "Usuário AMCash");

        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("usuario@amcash.com", jwtService.extractEmail(token));
        assertEquals("Usuário AMCash", jwtService.extractName(token));
    }

    @Test
    void shouldRejectAnInvalidToken() {
        assertFalse(jwtService.validateToken("token-invalido"));
    }
}
