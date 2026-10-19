package br.com.amcash.home.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.dto.response.CustomGoalResponse;
import br.com.amcash.goal.entity.CustomGoalPeriod;
import br.com.amcash.goal.entity.PrimaryGoalType;
import br.com.amcash.goal.service.CustomGoalService;
import br.com.amcash.goal.service.GoalService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeServiceTests {

    @Test
    void shouldReturnTheGoalDailyTargetBalanceAndNextPendingExpense() {
        GoalService goalService = mock(GoalService.class);
        CustomGoalService customGoalService = mock(CustomGoalService.class);
        FinancialEntryRepository entryRepository = mock(FinancialEntryRepository.class);
        HomeService service = new HomeService(goalService, customGoalService, entryRepository);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 18);
        GoalResponse goal = new GoalResponse(2026, 10, date,
                money("1000"), money("3558.60"), money("1000"), money("2558.60"),
                money("1558.60"), money("779.30"), money("182.76"), 14,
                money("39.10"), false, true, List.of(18, 19));
        FinancialEntry nextExpense = mock(FinancialEntry.class);
        UUID expenseId = UUID.randomUUID();
        when(nextExpense.getId()).thenReturn(expenseId);
        when(nextExpense.getName()).thenReturn("Internet");
        when(nextExpense.getDueDate()).thenReturn(LocalDate.of(2026, 10, 23));
        when(nextExpense.getAmount()).thenReturn(money("329"));
        when(goalService.current(userId, date)).thenReturn(goal);
        when(customGoalService.currentForHome(userId, date)).thenReturn(Optional.empty());
        when(entryRepository.findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.of(nextExpense));
        when(entryRepository.countByUserIdAndTypeAndPaidFalseAndDueDateBetween(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(12L);

        var response = service.current(userId, date);

        assertEquals(money("182.76"), response.goal().dailyTarget());
        assertTrue(response.hasFinancialEntries());
        assertEquals(PrimaryGoalType.EXPENSES, response.goal().type());
        assertEquals(money("-1558.60"), response.financial().balance());
        assertEquals(12L, response.financial().pendingExpenseCount());
        assertEquals(expenseId, response.nextExpense().id());
        assertEquals(money("329.00"), response.nextExpense().amount());
    }

    @Test
    void shouldIdentifyAMonthWithoutFinancialEntries() {
        GoalService goalService = mock(GoalService.class);
        CustomGoalService customGoalService = mock(CustomGoalService.class);
        FinancialEntryRepository entryRepository = mock(FinancialEntryRepository.class);
        HomeService service = new HomeService(goalService, customGoalService, entryRepository);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 18);
        GoalResponse goal = new GoalResponse(2026, 10, date,
                money("0"), money("0"), money("0"), money("0"),
                money("0"), money("0"), money("0"), 0,
                money("100"), true, false, List.of());
        when(goalService.current(userId, date)).thenReturn(goal);
        when(customGoalService.currentForHome(userId, date)).thenReturn(Optional.empty());
        when(entryRepository.findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.empty());

        var response = service.current(userId, date);

        assertFalse(response.hasFinancialEntries());
        assertTrue(response.goal().covered());
        assertEquals(money("0.00"), response.financial().balance());
        assertNull(response.nextExpense());
    }

    @Test
    void shouldExposeTheSelectedCustomGoalWithoutChangingFinancialSummary() {
        GoalService goalService = mock(GoalService.class);
        CustomGoalService customGoalService = mock(CustomGoalService.class);
        FinancialEntryRepository entryRepository = mock(FinancialEntryRepository.class);
        HomeService service = new HomeService(goalService, customGoalService, entryRepository);
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 18);
        GoalResponse expenseGoal = new GoalResponse(2026, 10, date,
                money("0"), money("2558.60"), money("0"), money("2558.60"),
                money("2558.60"), money("1279.30"), money("182.76"), 14,
                money("0"), false, true, List.of(18, 19));
        CustomGoalResponse customGoal = new CustomGoalResponse(goalId, "Meta Uber", money("6000"),
                CustomGoalPeriod.MONTHLY, date, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                money("300"), money("100"), money("200"), money("5800"), money("446.15"),
                money("461.54"), money("261.54"), 13, money("3.33"), false, true, List.of(18, 19));
        when(goalService.current(userId, date)).thenReturn(expenseGoal);
        when(customGoalService.currentForHome(userId, date)).thenReturn(Optional.of(customGoal));
        when(entryRepository.findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.empty());

        var response = service.current(userId, date);

        assertEquals(PrimaryGoalType.CUSTOM, response.goal().type());
        assertEquals("Meta Uber", response.goal().name());
        assertEquals(money("461.54"), response.goal().dailyTarget());
        assertEquals(money("5800.00"), response.goal().periodRemaining());
        assertEquals(money("-2558.60"), response.financial().balance());
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
