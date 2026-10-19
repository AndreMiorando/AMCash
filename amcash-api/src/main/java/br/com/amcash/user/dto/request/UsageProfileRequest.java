package br.com.amcash.user.dto.request;

import br.com.amcash.user.entity.UsageProfile;
import jakarta.validation.constraints.NotNull;

public record UsageProfileRequest(
        @NotNull(message = "Selecione um perfil de uso") UsageProfile usageProfile
) {
}
