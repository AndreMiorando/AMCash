package br.com.amcash.config;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        String name
) {
}
