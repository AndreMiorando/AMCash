package br.com.amcash.user.dto.response;

import br.com.amcash.user.entity.UsageProfile;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        String pictureUrl,
        UsageProfile usageProfile
) {
}
