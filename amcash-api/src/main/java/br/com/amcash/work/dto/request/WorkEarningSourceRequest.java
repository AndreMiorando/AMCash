package br.com.amcash.work.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkEarningSourceRequest(
        @NotBlank(message = "Informe o nome do ganho")
        @Size(max = 30, message = "O ganho deve ter no máximo 30 caracteres")
        String name) {
}
