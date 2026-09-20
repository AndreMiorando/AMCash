package br.com.amcash.auth.dto.response;

import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String name,
        String email,
        String pictureUrl
) {
}
