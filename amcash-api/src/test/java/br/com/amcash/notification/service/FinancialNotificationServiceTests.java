package br.com.amcash.notification.service;

import br.com.amcash.entry.entity.*;
import br.com.amcash.notification.entity.FinancialAlertType;
import br.com.amcash.notification.entity.FinancialNotification;
import br.com.amcash.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class FinancialNotificationServiceTests {
    @Test
    void shouldClassifyAndPrioritizeDueDates() {
        LocalDate today = LocalDate.of(2026, 10, 23);

        assertEquals(FinancialAlertType.OVERDUE, FinancialNotificationService.typeFor(today.minusDays(1), today));
        assertEquals(FinancialAlertType.UPCOMING, FinancialNotificationService.typeFor(today, today));
        assertEquals(FinancialAlertType.UPCOMING, FinancialNotificationService.typeFor(today.plusDays(3), today));
        assertEquals(0, FinancialNotificationService.group(FinancialAlertType.OVERDUE, today.minusDays(1), today));
        assertEquals(1, FinancialNotificationService.group(FinancialAlertType.UPCOMING, today, today));
        assertEquals(2, FinancialNotificationService.group(FinancialAlertType.UPCOMING, today.plusDays(1), today));
    }

    @Test
    void shouldKeepReadingOnReprocessingAndResetItAfterCategoryChange() {
        User user = new User("subject", "user@example.com", "User", null);
        FinancialEntry entry = new FinancialEntry(user, "Nubank", EntryCategory.BILLS_AND_SERVICES,
                EntryType.EXPENSE, new BigDecimal("100.00"), LocalDate.of(2026, 10, 24),
                RecurrenceFrequency.NONE, 0, 0, null, false);
        FinancialNotification notification = new FinancialNotification(user, entry, FinancialAlertType.UPCOMING);
        OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC);
        notification.markRead(readAt);

        notification.activate(FinancialAlertType.UPCOMING);
        assertEquals(readAt, notification.getReadAt());

        notification.activate(FinancialAlertType.OVERDUE);
        assertNull(notification.getReadAt());
        assertEquals(FinancialAlertType.OVERDUE, notification.getAlertType());
    }
}
