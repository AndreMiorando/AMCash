package br.com.amcash.goal.service;

import br.com.amcash.entry.service.MonthlyFinancialSnapshotService;
import br.com.amcash.entry.service.MonthlyFinancialSnapshotService.MonthlyFinancialSnapshot;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalServiceTests {
    private MonthlyFinancialSnapshotService financialService;
    private UserRepository userRepository;
    private GoalMonthPreferenceRepository preferenceRepository;
    private GoalService service;

    @BeforeEach
    void setUp() {
        financialService = mock(MonthlyFinancialSnapshotService.class);
        userRepository = mock(UserRepository.class);
        preferenceRepository = mock(GoalMonthPreferenceRepository.class);
        service = new GoalService(financialService, userRepository, preferenceRepository);
    }

    @Test
    void shouldCalculateGoalOnlyFromPendingExpenses() {
        UUID userId = UUID.randomUUID();
        stub(userId, YearMonth.of(2026, 10), financial("3000", "7000", "2000", "5000"));

        var response = service.current(userId, LocalDate.of(2026, 10, 18));

        assertEquals(new BigDecimal("2000.00"), response.remainingAmount());
        assertEquals(new BigDecimal("2000.00"), response.paidExpenses());
        assertEquals(new BigDecimal("5000.00"), response.pendingExpenses());
    }

    @Test
    void workResultMustNotChangeTheGoalCalculation() {
        UUID userId = UUID.randomUUID();
        stub(userId, YearMonth.of(2026, 10), financial("1000", "5000", "0", "5000"));

        var response = service.current(userId, LocalDate.of(2026, 10, 18));

        assertEquals(new BigDecimal("4000.00"), response.remainingAmount());
    }

    @Test
    void dividesCurrentTargetBetweenRemainingSelectedDays() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        YearMonth month = YearMonth.of(2026, 10);
        GoalMonthPreference preference = new GoalMonthPreference(user, 2026, 10, 0);
        preference.updateSelectedDaysMask((1 << 17) | (1 << 18));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 10)).thenReturn(Optional.of(preference));
        when(financialService.load(userId, month)).thenReturn(financial("0", "1000", "0", "1000"));

        var response = service.current(userId, LocalDate.of(2026, 10, 18));

        assertEquals(new BigDecimal("500.00"), response.dailyTarget());
        assertEquals(new BigDecimal("1000.00"), response.remainingAmount());
        assertTrue(response.todaySelected());
    }

    @Test
    void startsEachMonthWithoutSelectedDaysAndCanPersistThem() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 11);
        stub(userId, month, financial("0", "900", "0", "900"));
        assertEquals(List.of(), service.current(userId, LocalDate.of(2026, 10, 18), month).selectedDays());

        var updated = service.updatePreferences(userId, LocalDate.of(2026, 10, 18), month,
                new UpdateGoalPreferencesRequest(Set.of(1, 2, 3)));
        assertEquals(3, updated.availableDays());
        assertEquals(new BigDecimal("300.00"), updated.dailyTarget());
        assertFalse(updated.todaySelected());
        verify(preferenceRepository).save(any(GoalMonthPreference.class));
    }

    @Test
    void rejectsDayOutsideMonth() {
        assertThrows(BadRequestException.class, () -> service.updatePreferences(UUID.randomUUID(),
                LocalDate.of(2026, 9, 1), new UpdateGoalPreferencesRequest(Set.of(31))));
        verify(userRepository, never()).findById(any());
    }

    private void stub(UUID userId, YearMonth month, MonthlyFinancialSnapshot financial) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(financialService.load(userId, month)).thenReturn(financial);
    }

    private MonthlyFinancialSnapshot financial(String income, String total, String paid, String pending) {
        return new MonthlyFinancialSnapshot(new BigDecimal(income), new BigDecimal(total),
                new BigDecimal(paid), new BigDecimal(pending));
    }

    private User user(UUID id) {
        User user = new User("google-subject", "usuario@gmail.com", "Usuário", null);
        user.setId(id);
        return user;
    }
}
