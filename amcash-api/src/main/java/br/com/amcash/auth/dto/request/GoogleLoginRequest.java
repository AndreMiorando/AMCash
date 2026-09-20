package br.com.amcash.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "O token do Google é obrigatório")
        String idToken
) {
}
