package br.com.amcash.home.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.entity.CustomGoalPeriod;
import br.com.amcash.goal.entity.PrimaryGoalType;
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
    void returnsUnifiedGoalAndNextPendingExpense() {
        GoalService goalService = mock(GoalService.class);
        FinancialEntryRepository entryRepository = mock(FinancialEntryRepository.class);
        HomeService service = new HomeService(goalService, entryRepository);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 18);
        GoalResponse goal = goal(date, "2558.60", "500", "3058.60", "2800", "182.76", false, true);
        FinancialEntry nextExpense = mock(FinancialEntry.class);
        UUID expenseId = UUID.randomUUID();
        when(nextExpense.getId()).thenReturn(expenseId);
        when(nextExpense.getName()).thenReturn("Internet");
        when(nextExpense.getDueDate()).thenReturn(LocalDate.of(2026, 10, 23));
        when(nextExpense.getAmount()).thenReturn(money("329"));
        when(goalService.current(userId, date)).thenReturn(goal);
        when(entryRepository.findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.of(nextExpense));
        when(entryRepository.countByUserIdAndTypeAndPaidFalseAndDueDateBetween(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(12L);

        var response = service.current(userId, date);

        assertEquals(money("182.76"), response.goal().dailyTarget());
        assertEquals("Objetivo mensal", response.goal().name());
        assertEquals(CustomGoalPeriod.MONTHLY, response.goal().period());
        assertEquals(PrimaryGoalType.EXPENSES, response.goal().type());
        assertTrue(response.hasFinancialEntries());
        assertEquals(12L, response.financial().pendingExpenseCount());
        assertEquals(expenseId, response.nextExpense().id());
    }

    @Test
    void supportsAdditionalGoalWithoutFinancialEntries() {
        GoalService goalService = mock(GoalService.class);
        FinancialEntryRepository entryRepository = mock(FinancialEntryRepository.class);
        HomeService service = new HomeService(goalService, entryRepository);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 18);
        when(goalService.current(userId, date)).thenReturn(
                goal(date, "0", "1200", "1200", "1200", "100", false, true));
        when(entryRepository.findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.empty());

        var response = service.current(userId, date);

        assertFalse(response.hasFinancialEntries());
        assertFalse(response.goal().covered());
        assertEquals(money("1200"), response.goal().periodRemaining());
        assertNull(response.nextExpense());
    }

    private GoalResponse goal(LocalDate date, String pending, String additional, String target,
                              String remaining, String todayTarget, boolean covered, boolean todaySelected) {
        return new GoalResponse(2026, 10, date, money("0"), money(pending), money("0"), money(pending),
                true, money(pending), money(additional), money(target), money("0"), money(remaining),
                money("500"), money("200"), money(todayTarget), money(todayTarget), 14,
                money("0"), covered, todaySelected, List.of(18, 19));
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
