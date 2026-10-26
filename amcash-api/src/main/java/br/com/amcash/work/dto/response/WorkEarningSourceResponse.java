package br.com.amcash.work.dto.response;

import java.util.UUID;

public record WorkEarningSourceResponse(
        UUID id,
        String name,
        String systemKey,
        boolean active,
        int sortOrder) {
}
