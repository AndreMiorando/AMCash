package br.com.amcash.auth.dto.response;

import br.com.amcash.user.entity.UsageProfile;

import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String name,
        String email,
        String pictureUrl,
        UsageProfile usageProfile
) {
}
