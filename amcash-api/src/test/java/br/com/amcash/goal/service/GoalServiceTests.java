package br.com.amcash.goal.service;

import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalServiceTests {

    private FinancialEntryRepository entryRepository;
    private SubexpenseRepository subexpenseRepository;
    private UserRepository userRepository;
    private GoalService goalService;

    @BeforeEach
    void setUp() {
        entryRepository = mock(FinancialEntryRepository.class);
        subexpenseRepository = mock(SubexpenseRepository.class);
        userRepository = mock(UserRepository.class);
        goalService = new GoalService(entryRepository, subexpenseRepository, userRepository);
    }

    @Test
    void shouldCalculateDynamicGoalFromIncomePendingExpensesAndAvailableDays() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        FinancialEntry income = entry(user, EntryType.INCOME, "3000.00", false);
        FinancialEntry pending = entry(user, EntryType.EXPENSE, "3000.00", false);
        FinancialEntry paid = entry(user, EntryType.EXPENSE, "1000.00", false);
        paid.setPaid(true);
        FinancialEntry detailed = entry(user, EntryType.EXPENSE, "3000.00", true);
        Subexpense paidItem = new Subexpense(
                detailed, "Streaming", new BigDecimal("1000.00"), null, true);
        Subexpense pendingItem = new Subexpense(
                detailed, "Notebook", new BigDecimal("2000.00"), null, false);
        paidItem.setId(UUID.randomUUID());
        pendingItem.setId(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(income, pending, paid, detailed));
        when(subexpenseRepository.findAllByEntryIdInOrderByEntryIdAscCreatedAtAsc(
                List.of(detailed.getId())))
                .thenReturn(List.of(paidItem, pendingItem));

        var response = goalService.current(userId, LocalDate.of(2026, 9, 28));

        assertEquals(new BigDecimal("3000.00"), response.income());
        assertEquals(new BigDecimal("5000.00"), response.pendingExpenses());
        assertEquals(new BigDecimal("2000.00"), response.remainingAmount());
        assertEquals(3, response.availableDays());
        assertEquals(new BigDecimal("666.67"), response.dailyTarget());
        assertEquals(new BigDecimal("2000.00"), response.weeklyTarget());
        assertEquals(new BigDecimal("60.00"), response.progressPercentage());
        assertEquals(List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY), response.availableWeekdays());
    }

    @Test
    void shouldPersistAvailableWeekdaysAndRecalculateTargets() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        FinancialEntry expense = entry(user, EntryType.EXPENSE, "1000.00", false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(expense));

        var response = goalService.updatePreferences(
                userId,
                LocalDate.of(2026, 9, 26),
                new UpdateGoalPreferencesRequest(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));

        assertEquals(96, user.getGoalWeekdaysMask());
        assertEquals(2, response.availableDays());
        assertEquals(new BigDecimal("500.00"), response.dailyTarget());
        assertEquals(new BigDecimal("1000.00"), response.weeklyTarget());
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectEmptyAvailableWeekdays() {
        assertThrows(BadRequestException.class, () -> goalService.updatePreferences(
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 1),
                new UpdateGoalPreferencesRequest(Set.of())));
        verify(userRepository, never()).save(any());
    }

    private User user(UUID id) {
        User user = new User("google-subject", "usuario@gmail.com", "Usuário", null);
        user.setId(id);
        return user;
    }

    private FinancialEntry entry(User user, EntryType type, String amount, boolean detailed) {
        FinancialEntry entry = new FinancialEntry(
                user,
                type == EntryType.INCOME ? "Receita" : "Despesa",
                type == EntryType.INCOME ? EntryCategory.OTHER_INCOME : EntryCategory.OTHER_EXPENSE,
                type,
                new BigDecimal(amount),
                LocalDate.of(2026, 9, 10),
                RecurrenceFrequency.NONE,
                0,
                0,
                null,
                detailed);
        entry.setId(UUID.randomUUID());
        return entry;
    }
}
