package br.com.amcash.goal.service;

import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.entity.GoalMonthPreference;
import br.com.amcash.goal.repository.GoalMonthPreferenceRepository;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private GoalMonthPreferenceRepository goalPreferenceRepository;
    private GoalService goalService;

    @BeforeEach
    void setUp() {
        entryRepository = mock(FinancialEntryRepository.class);
        subexpenseRepository = mock(SubexpenseRepository.class);
        userRepository = mock(UserRepository.class);
        goalPreferenceRepository = mock(GoalMonthPreferenceRepository.class);
        goalService = new GoalService(
                entryRepository,
                subexpenseRepository,
                userRepository,
                goalPreferenceRepository);
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
        assertEquals(0, response.availableDays());
        assertEquals(new BigDecimal("2000.00"), response.dailyTarget());
        assertEquals(new BigDecimal("2000.00"), response.weeklyTarget());
        assertEquals(new BigDecimal("60.00"), response.progressPercentage());
        assertEquals(List.of(), response.selectedDays());
    }

    @Test
    void shouldPersistSelectedDaysAndRecalculateTargets() {
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
                new UpdateGoalPreferencesRequest(Set.of(27, 28)));

        assertEquals(2, response.availableDays());
        assertEquals(new BigDecimal("500.00"), response.dailyTarget());
        assertEquals(new BigDecimal("1000.00"), response.weeklyTarget());
        assertEquals(List.of(27, 28), response.selectedDays());
        verify(goalPreferenceRepository).save(any(GoalMonthPreference.class));
    }

    @Test
    void shouldNotCountTodayAsAnAvailableDay() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        GoalMonthPreference preference = new GoalMonthPreference(
                user,
                2026,
                9,
                GoalMonthPreference.ALL_WEEKDAYS_MASK);
        preference.updateSelectedDaysMask(1 << (27 - 1));
        FinancialEntry expense = entry(user, EntryType.EXPENSE, "1000.00", false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalPreferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 9))
                .thenReturn(Optional.of(preference));
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(expense));

        var response = goalService.current(userId, LocalDate.of(2026, 9, 27));

        assertEquals(0, response.availableDays());
        assertEquals(new BigDecimal("1000.00"), response.dailyTarget());
        assertEquals(new BigDecimal("1000.00"), response.weeklyTarget());
    }

    @Test
    void shouldStartTheFollowingMonthWithoutSelectedDays() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        FinancialEntry expense = entry(user, EntryType.EXPENSE, "2200.00", false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(expense));

        var response = goalService.current(
                userId,
                LocalDate.of(2026, 9, 27),
                YearMonth.of(2026, 10));

        assertEquals(2026, response.year());
        assertEquals(10, response.month());
        assertEquals(0, response.availableDays());
        assertEquals(new BigDecimal("2200.00"), response.dailyTarget());
        assertEquals(new BigDecimal("2200.00"), response.weeklyTarget());
        assertEquals(List.of(), response.selectedDays());
    }

    @Test
    void shouldDivideCurrentMonthTargetOnlyByDaysAfterToday() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        FinancialEntry expense = entry(user, EntryType.EXPENSE, "2800.00", false);
        GoalMonthPreference preference = new GoalMonthPreference(
                user,
                2026,
                10,
                GoalMonthPreference.ALL_WEEKDAYS_MASK);
        preference.updateSelectedDaysMask(Integer.MAX_VALUE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalPreferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 10))
                .thenReturn(Optional.of(preference));
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(expense));

        var response = goalService.current(userId, LocalDate.of(2026, 10, 3));

        assertEquals(28, response.availableDays());
        assertEquals(new BigDecimal("100.00"), response.dailyTarget());
        assertEquals(new BigDecimal("700.00"), response.weeklyTarget());
    }

    @Test
    void shouldNotReuseDaysSelectedForAnotherMonth() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        GoalMonthPreference septemberPreference = new GoalMonthPreference(
                user,
                2026,
                9,
                7);
        septemberPreference.updateSelectedDaysMask((1 << (28 - 1)) | (1 << (29 - 1)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(goalPreferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 9))
                .thenReturn(Optional.of(septemberPreference));
        when(goalPreferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 10))
                .thenReturn(Optional.empty());
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of());

        var october = goalService.current(
                userId,
                LocalDate.of(2026, 9, 27),
                YearMonth.of(2026, 10));

        assertEquals(List.of(), october.selectedDays());
        assertEquals(0, october.availableDays());
    }

    @Test
    void shouldAllowClearingAllSelectedDays() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        FinancialEntry expense = entry(user, EntryType.EXPENSE, "1000.00", false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(expense));

        var response = goalService.updatePreferences(
                userId,
                LocalDate.of(2026, 9, 1),
                new UpdateGoalPreferencesRequest(Set.of()));

        assertEquals(0, response.availableDays());
        assertEquals(new BigDecimal("1000.00"), response.dailyTarget());
        assertEquals(new BigDecimal("1000.00"), response.weeklyTarget());
        assertEquals(List.of(), response.selectedDays());
        verify(goalPreferenceRepository).save(any(GoalMonthPreference.class));
    }

    @Test
    void shouldRejectADayOutsideTheSelectedMonth() {
        assertThrows(BadRequestException.class, () -> goalService.updatePreferences(
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 1),
                new UpdateGoalPreferencesRequest(Set.of(31))));
        verify(userRepository, never()).findById(any());
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
