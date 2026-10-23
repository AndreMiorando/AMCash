package br.com.amcash.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        UUID entryId,
        String expenseName,
        BigDecimal amount,
        LocalDate dueDate,
        OffsetDateTime createdAt,
        OffsetDateTime readAt) {}
