package br.com.amcash.goal.service;

import br.com.amcash.goal.dto.request.CustomGoalRequest;
import br.com.amcash.goal.entity.CustomGoal;
import br.com.amcash.goal.entity.CustomGoalPeriod;
import br.com.amcash.goal.entity.GoalMonthPreference;
import br.com.amcash.goal.entity.PrimaryGoalType;
import br.com.amcash.goal.repository.CustomGoalRepository;
import br.com.amcash.goal.repository.GoalMonthPreferenceRepository;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.service.WorkService;
import br.com.amcash.work.service.WorkService.WorkResultProgressSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class CustomGoalServiceTests {
    private CustomGoalRepository repository;
    private GoalMonthPreferenceRepository preferenceRepository;
    private UserRepository userRepository;
    private WorkService workService;
    private CustomGoalService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(CustomGoalRepository.class);
        preferenceRepository = mock(GoalMonthPreferenceRepository.class);
        userRepository = mock(UserRepository.class);
        workService = mock(WorkService.class);
        service = new CustomGoalService(repository, preferenceRepository, userRepository, workService);
        userId = UUID.randomUUID();
        user = user(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(CustomGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workService.resultProgress(any(), any(), any(), any())).thenReturn(result("0", "0"));
    }

    @ParameterizedTest
    @EnumSource(CustomGoalPeriod.class)
    void createsDailyWeeklyAndMonthlyGoals(CustomGoalPeriod period) {
        var response = service.create(userId, LocalDate.of(2026, 10, 18), request("Meta", "1500", period));

        assertEquals(period, response.period());
        assertEquals(new BigDecimal("1500.00"), response.amount());
        assertEquals(new BigDecimal("1500.00"), response.remainingAmount());
        ArgumentCaptor<CustomGoal> savedGoal = ArgumentCaptor.forClass(CustomGoal.class);
        verify(repository).save(savedGoal.capture());
        assertTrue(savedGoal.getValue().isTrackedOnHome());
    }

    @Test
    void recalculatesWeeklyGoalAfterResultBelowExpected() {
        CustomGoal goal = goal("1500", CustomGoalPeriod.WEEKLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        selectDays(14, 15, 16, 17, 18);
        when(workService.resultProgress(userId, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14)))
                .thenReturn(result("300", "100"));

        var response = service.current(userId, LocalDate.of(2026, 10, 14)).orElseThrow();

        assertEquals(new BigDecimal("200.00"), response.netResult());
        assertEquals(new BigDecimal("1300.00"), response.remainingAmount());
        assertEquals(new BigDecimal("260.00"), response.dailyTarget());
    }

    @Test
    void recalculatesMonthlyGoalAfterResultAboveExpected() {
        CustomGoal goal = goal("6000", CustomGoalPeriod.MONTHLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        selectDays(19, 20, 21, 22, 23, 24, 25, 26, 27, 28);
        when(workService.resultProgress(userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18)))
                .thenReturn(result("1400", "100"));

        var response = service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow();

        assertEquals(new BigDecimal("4700.00"), response.remainingAmount());
        assertEquals(new BigDecimal("470.00"), response.dailyTarget());
    }

    @Test
    void negativeNetResultIncreasesRemainingAmount() {
        CustomGoal goal = goal("250", CustomGoalPeriod.DAILY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(workService.resultProgress(userId, LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18)))
                .thenReturn(result("100", "180"));

        var response = service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow();

        assertEquals(new BigDecimal("-80.00"), response.netResult());
        assertEquals(new BigDecimal("330.00"), response.remainingAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), response.progressPercentage());
    }

    @Test
    void dayWithoutRevenueKeepsOriginalDailyGoal() {
        CustomGoal goal = goal("250", CustomGoalPeriod.DAILY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));

        var response = service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow();

        assertEquals(new BigDecimal("250.00"), response.dailyTarget());
        assertEquals(new BigDecimal("250.00"), response.remainingAmount());
    }

    @Test
    void noSelectedWorkDayDoesNotDivideByZero() {
        CustomGoal goal = goal("1000", CustomGoalPeriod.MONTHLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));

        var response = service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow();

        assertEquals(0, response.availableDays());
        assertEquals(new BigDecimal("1000.00"), response.dailyTarget());
        assertFalse(response.todaySelected());
    }

    @Test
    void achievedGoalHasNoNegativeRemainingTarget() {
        CustomGoal goal = goal("1000", CustomGoalPeriod.MONTHLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(workService.resultProgress(userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18)))
                .thenReturn(result("1400", "100"));

        var response = service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow();

        assertTrue(response.achieved());
        assertEquals(new BigDecimal("0.00"), response.remainingAmount());
        assertEquals(new BigDecimal("0.00"), response.dailyTarget());
        assertEquals(new BigDecimal("100.00"), response.progressPercentage());
    }

    @Test
    void recalculatesWheneverWorkResultChanges() {
        CustomGoal goal = goal("1000", CustomGoalPeriod.MONTHLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(workService.resultProgress(userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18)))
                .thenReturn(result("300", "50"), result("500", "50"));

        assertEquals(new BigDecimal("750.00"), service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow().remainingAmount());
        assertEquals(new BigDecimal("550.00"), service.current(userId, LocalDate.of(2026, 10, 18)).orElseThrow().remainingAmount());
    }

    @Test
    void updateAndDeleteAreScopedToAuthenticatedUser() {
        CustomGoal goal = goal("1000", CustomGoalPeriod.MONTHLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));

        service.update(userId, LocalDate.of(2026, 10, 18), request("Nova meta", "1200", CustomGoalPeriod.WEEKLY));
        service.delete(userId);

        assertEquals("Nova meta", goal.getName());
        assertEquals(CustomGoalPeriod.WEEKLY, goal.getPeriod());
        verify(repository).delete(goal);
        UUID anotherUser = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.delete(anotherUser));
    }

    @Test
    void preventsMoreThanOneActiveGoalPerUser() {
        when(repository.existsByUserId(userId)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.create(userId, LocalDate.of(2026, 10, 18),
                request("Meta", "500", CustomGoalPeriod.DAILY)));
    }

    @Test
    void persistsTheGoalSelectedForHomeAndFallsBackToExpensesAfterDeletion() {
        CustomGoal goal = goal("1000", CustomGoalPeriod.MONTHLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        when(repository.findByUserIdAndTrackedOnHomeTrue(userId))
                .thenReturn(Optional.empty(), Optional.of(goal), Optional.empty());

        assertEquals(PrimaryGoalType.EXPENSES, service.primary(userId));
        assertEquals(PrimaryGoalType.CUSTOM, service.selectPrimary(userId, PrimaryGoalType.CUSTOM));
        assertTrue(goal.isTrackedOnHome());
        assertEquals(PrimaryGoalType.CUSTOM, service.primary(userId));
        assertEquals(PrimaryGoalType.EXPENSES, service.selectPrimary(userId, PrimaryGoalType.EXPENSES));
        assertFalse(goal.isTrackedOnHome());
        assertEquals(PrimaryGoalType.CUSTOM, service.selectPrimary(userId, PrimaryGoalType.CUSTOM));

        service.delete(userId);
        assertEquals(PrimaryGoalType.EXPENSES, service.primary(userId));
    }

    @Test
    void keepsTodayTargetStableWhileTodayResultChanges() {
        CustomGoal goal = goal("1500", CustomGoalPeriod.WEEKLY);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(goal));
        selectDays(14, 15, 16, 17, 18);
        when(workService.resultProgress(userId, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14)))
                .thenReturn(result("300", "100", "300", "100"));

        var response = service.current(userId, LocalDate.of(2026, 10, 14)).orElseThrow();

        assertEquals(new BigDecimal("300.00"), response.todayTarget());
        assertEquals(new BigDecimal("100.00"), response.todayRemaining());
        assertEquals(new BigDecimal("260.00"), response.dailyTarget());
    }

    private CustomGoal goal(String amount, CustomGoalPeriod period) {
        return new CustomGoal(user, "Minha meta", new BigDecimal(amount), period);
    }

    private CustomGoalRequest request(String name, String amount, CustomGoalPeriod period) {
        return new CustomGoalRequest(name, new BigDecimal(amount), period);
    }

    private WorkResultProgressSnapshot result(String gross, String costs) {
        return result(gross, costs, gross, costs);
    }

    private WorkResultProgressSnapshot result(String gross, String costs, String todayGross, String todayCosts) {
        BigDecimal grossValue = new BigDecimal(gross).setScale(2);
        BigDecimal costValue = new BigDecimal(costs).setScale(2);
        BigDecimal todayGrossValue = new BigDecimal(todayGross).setScale(2);
        BigDecimal todayCostValue = new BigDecimal(todayCosts).setScale(2);
        return new WorkResultProgressSnapshot(grossValue, costValue, grossValue.subtract(costValue),
                todayGrossValue, todayCostValue, todayGrossValue.subtract(todayCostValue));
    }

    private void selectDays(Integer... days) {
        GoalMonthPreference preference = new GoalMonthPreference(user, 2026, 10, 0);
        int mask = Set.of(days).stream().mapToInt(day -> 1 << (day - 1)).reduce(0, (left, right) -> left | right);
        preference.updateSelectedDaysMask(mask);
        when(preferenceRepository.findByUserIdAndYearAndMonth(userId, 2026, 10)).thenReturn(Optional.of(preference));
    }

    private User user(UUID id) {
        User created = new User("google-subject", "usuario@gmail.com", "Usuário", null);
        created.setId(id);
        return created;
    }
}
