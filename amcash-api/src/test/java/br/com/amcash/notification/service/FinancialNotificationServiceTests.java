package br.com.amcash.notification.service;

import br.com.amcash.entry.entity.*;
import br.com.amcash.notification.entity.FinancialAlertType;
import br.com.amcash.notification.entity.FinancialNotification;
import br.com.amcash.notification.repository.FinancialNotificationRepository;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Test
    void shouldKeepOverdueExpenseActiveAfterReadingAndResolveItOnlyWhenNoLongerPending() {
        FinancialEntryRepository entries = mock(FinancialEntryRepository.class);
        FinancialNotificationRepository notifications = mock(FinancialNotificationRepository.class);
        FinancialNotificationService service = new FinancialNotificationService(entries, notifications);
        User user = new User("subject", "user@example.com", "User", null);
        user.setId(UUID.randomUUID());
        FinancialEntry entry = new FinancialEntry(user, "Internet", EntryCategory.BILLS_AND_SERVICES,
                EntryType.EXPENSE, new BigDecimal("120.00"), LocalDate.now().minusDays(2),
                RecurrenceFrequency.NONE, 0, 0, null, false);
        entry.setId(UUID.randomUUID());
        FinancialNotification notification = new FinancialNotification(user, entry, FinancialAlertType.OVERDUE);

        when(entries.findAllByUserIdAndTypeAndPaidFalseAndDueDateLessThanEqualOrderByDueDateAscCreatedAtAsc(
                eq(user.getId()), eq(EntryType.EXPENSE), any(LocalDate.class))).thenReturn(List.of(entry));
        when(notifications.findAllByUserIdAndActiveTrue(user.getId())).thenReturn(List.of(notification));
        when(notifications.findByUserIdAndEntryId(user.getId(), entry.getId())).thenReturn(Optional.of(notification));

        service.markAllRead(user.getId());

        assertTrue(notification.isActive());
        assertNotNull(notification.getReadAt());

        when(entries.findAllByUserIdAndTypeAndPaidFalseAndDueDateLessThanEqualOrderByDueDateAscCreatedAtAsc(
                eq(user.getId()), eq(EntryType.EXPENSE), any(LocalDate.class))).thenReturn(List.of());
        service.synchronize(user.getId());

        assertFalse(notification.isActive());
    }
}
