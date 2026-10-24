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
        OffsetDateTime readAt,
        String source,
        UUID vehicleId,
        String message,
        String suggestionSource) {
    public NotificationResponse(UUID id, String type, UUID entryId, String expenseName, BigDecimal amount,
                                LocalDate dueDate, OffsetDateTime createdAt, OffsetDateTime readAt) {
        this(id, type, entryId, expenseName, amount, dueDate, createdAt, readAt,
                "FINANCIAL", null, null, null);
    }
    public NotificationResponse(UUID id,String type,UUID entryId,String expenseName,BigDecimal amount,LocalDate dueDate,OffsetDateTime createdAt,OffsetDateTime readAt,String source,UUID vehicleId,String message){this(id,type,entryId,expenseName,amount,dueDate,createdAt,readAt,source,vehicleId,message,null);}
}
