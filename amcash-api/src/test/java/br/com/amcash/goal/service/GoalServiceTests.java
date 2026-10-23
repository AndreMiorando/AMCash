package br.com.amcash.goal.service;

import br.com.amcash.entry.service.MonthlyFinancialSnapshotService;
import br.com.amcash.entry.service.MonthlyFinancialSnapshotService.MonthlyFinancialSnapshot;
import br.com.amcash.goal.dto.request.UpdateGoalConfigurationRequest;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.entity.GoalMonthPreference;
import br.com.amcash.goal.entity.MonthlyGoalConfiguration;
import br.com.amcash.goal.repository.GoalMonthPreferenceRepository;
import br.com.amcash.goal.repository.MonthlyGoalConfigurationRepository;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.service.WorkService;
import br.com.amcash.work.service.WorkService.WorkProgressSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalServiceTests {
    private MonthlyFinancialSnapshotService financialService;
    private UserRepository userRepository;
    private GoalMonthPreferenceRepository preferenceRepository;
    private MonthlyGoalConfigurationRepository configurationRepository;
    private WorkService workService;
    private GoalService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        financialService = mock(MonthlyFinancialSnapshotService.class);
        userRepository = mock(UserRepository.class);
        preferenceRepository = mock(GoalMonthPreferenceRepository.class);
        configurationRepository = mock(MonthlyGoalConfigurationRepository.class);
        workService = mock(WorkService.class);
        service = new GoalService(financialService, userRepository, preferenceRepository,
                configurationRepository, workService);
        userId = UUID.randomUUID();
        user = user(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(configurationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(configurationRepository.save(any(MonthlyGoalConfiguration.class))).thenAnswer(invocation -> {
            MonthlyGoalConfiguration saved = invocation.getArgument(0);
            when(configurationRepository.findByUserId(userId)).thenReturn(Optional.of(saved));
            return saved;
        });
        when(workService.progress(any(), any(), any())).thenReturn(progress("0", "0"));
    }

    @Test
    void defaultsToPendingExpensesWithoutPersistingDerivedTotals() {
        YearMonth month = YearMonth.of(2026, 10);
        when(financialService.load(userId, month)).thenReturn(financial("3000", "7000", "2000", "5000"));
        var response = service.current(userId, LocalDate.of(2026, 10, 18));
        assertTrue(response.includeExpenses());
        assertEquals(money("5000"), response.consideredExpenses());
        assertEquals(money("0"), response.additionalAmount());
        assertEquals(money("5000"), response.targetAmount());
        assertEquals(money("5000"), response.remainingAmount());
    }

    @Test
    void combinesPendingExpensesWithPersistentAdditionalAmount() {
        YearMonth month = YearMonth.of(2026, 10);
        MonthlyGoalConfiguration configuration = new MonthlyGoalConfiguration(user, true, new BigDecimal("750"));
        when(configurationRepository.findByUserId(userId)).thenReturn(Optional.of(configuration));
        when(financialService.load(userId, month)).thenReturn(financial("0", "2000", "500", "1500"));
        var response = service.current(userId, LocalDate.of(2026, 10, 18));
        assertEquals(money("1500"), response.consideredExpenses());
        assertEquals(money("750"), response.additionalAmount());
        assertEquals(money("2250"), response.targetAmount());
    }

    @Test
    void canCreateGoalWithoutExpensesAndDisableTheirInclusion() {
        YearMonth month = YearMonth.of(2026, 10);
        when(financialService.load(userId, month)).thenReturn(financial("0", "0", "0", "0"));
        var response = service.updateConfiguration(userId, LocalDate.of(2026, 10, 18),
                new UpdateGoalConfigurationRequest(false, new BigDecimal("1200")));
        assertFalse(response.includeExpenses());
        assertEquals(money("1200"), response.targetAmount());
        ArgumentCaptor<MonthlyGoalConfiguration> saved = ArgumentCaptor.forClass(MonthlyGoalConfiguration.class);
        verify(configurationRepository).save(saved.capture());
        assertFalse(saved.getValue().isIncludeExpenses());
        assertEquals(new BigDecimal("1200"), saved.getValue().getAdditionalAmount());
    }

    @Test
    void acceptsZeroAdditionalAmountWhenPendingExpensesAreIncluded() {
        YearMonth month = YearMonth.of(2026, 10);
        when(financialService.load(userId, month)).thenReturn(financial("0", "500", "0", "500"));
        var response = service.updateConfiguration(userId, LocalDate.of(2026, 10, 18),
                new UpdateGoalConfigurationRequest(true, BigDecimal.ZERO));
        assertEquals(money("500"), response.targetAmount());
    }

    @Test
    void rejectsConfigurationWithNoExpensesAndNoAdditionalAmount() {
        YearMonth month = YearMonth.of(2026, 10);
        when(financialService.load(userId, month)).thenReturn(financial("0", "0", "0", "0"));
        assertThrows(BadRequestException.class, () -> service.updateConfiguration(userId,
                LocalDate.of(2026, 10, 18), new UpdateGoalConfigurationRequest(false, BigDecimal.ZERO)));
    }

    @Test
    void recalculatesWhenPaymentChangesPendingExpensesWithoutChangingAdditionalAmount() {
        YearMonth month = YearMonth.of(2026, 10);
        MonthlyGoalConfiguration configuration = new MonthlyGoalConfiguration(user, true, new BigDecimal("200"));
        when(configurationRepository.findByUserId(userId)).thenReturn(Optional.of(configuration));
        when(financialService.load(userId, month)).thenReturn(
                financial("0", "500", "0", "500"), financial("0", "500", "200", "300"));
        when(workService.progress(userId, LocalDate.of(2026, 10, 18), month)).thenReturn(progress("100", "0"));
        assertEquals(money("600"), service.current(userId, LocalDate.of(2026, 10, 18)).remainingAmount());
        var afterPayment = service.current(userId, LocalDate.of(2026, 10, 18));
        assertEquals(money("400"), afterPayment.remainingAmount());
        assertEquals(money("200"), afterPayment.additionalAmount());
    }

    @Test
    void usesNetWorkResultAsRealizedAmountAndKeepsTodayTargetStable() {
        YearMonth month = YearMonth.of(2026, 10);
        when(financialService.load(userId, month)).thenReturn(financial("0", "1000", "0", "1000"));
        selectDays(18, 19);
        when(workService.progress(userId, LocalDate.of(2026, 10, 18), month))
                .thenReturn(new WorkProgressSnapshot(money("400"), money("100"), money("300"),
                        money("200"), money("50"), money("150")));
        var response = service.current(userId, LocalDate.of(2026, 10, 18));
        assertEquals(money("300"), response.realizedAmount());
        assertEquals(money("700"), response.remainingAmount());
        assertEquals(money("425"), response.todayTarget());
        assertEquals(money("275"), response.todayRemaining());
        assertEquals(money("350"), response.dailyTarget());
    }

    @Test
    void dividesCurrentTargetBetweenRemainingSelectedDays() {
        YearMonth month = YearMonth.of(2026, 10);
        when(financialService.load(userId, month)).thenReturn(financial("0", "1000", "0", "1000"));
        selectDays(18, 19);
        var response = service.current(userId, LocalDate.of(2026, 10, 18));
        assertEquals(money("500"), response.dailyTarget());
        assertEquals(2, response.availableDays());
        assertTrue(response.todaySelected());
    }

    @Test
    void startsEachMonthWithoutSelectedDaysAndCanPersistThem() {
        YearMonth month = YearMonth.of(2026, 11);
        when(financialService.load(userId, month)).thenReturn(financial("0", "900", "0", "900"));
        assertEquals(List.of(), service.current(userId, LocalDate.of(2026, 10, 18), month).selectedDays());
        var updated = service.updatePreferences(userId, LocalDate.of(2026, 10, 18), month,
                new UpdateGoalPreferencesRequest(Set.of(1, 2, 3)));
        assertEquals(3, updated.availableDays());
        assertEquals(money("300"), updated.dailyTarget());
        verify(preferenceRepository).save(any(GoalMonthPreference.class));
    }

    @Test
    void rejectsDayOutsideMonth() {
        assertThrows(BadRequestException.class, () -> service.updatePreferences(userId,
                LocalDate.of(2026, 9, 1), new UpdateGoalPreferencesRequest(Set.of(31))));
    }

    private void selectDays(Integer... days) {
        GoalMonthPreference preference = new GoalMonthPreference(user, 2026, 10, 0);
        int mask = Set.of(days).stream().mapToInt(day -> 1 << (day - 1)).reduce(0, (left, right) -> left | right);
        preference.updateSelectedDaysMask(mask);
        when(preferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 10)).thenReturn(Optional.of(preference));
    }

    private MonthlyFinancialSnapshot financial(String income, String total, String paid, String pending) {
        return new MonthlyFinancialSnapshot(new BigDecimal(income), new BigDecimal(total),
                new BigDecimal(paid), new BigDecimal(pending));
    }

    private WorkProgressSnapshot progress(String monthNet, String todayNet) {
        BigDecimal month = money(monthNet);
        BigDecimal today = money(todayNet);
        return new WorkProgressSnapshot(month, BigDecimal.ZERO.setScale(2), month,
                today, BigDecimal.ZERO.setScale(2), today);
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }

    private User user(UUID id) {
        User created = new User("google-subject", "usuario@gmail.com", "Usuário", null);
        created.setId(id);
        return created;
    }
}
