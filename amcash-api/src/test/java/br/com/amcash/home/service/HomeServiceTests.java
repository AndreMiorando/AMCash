package br.com.amcash.home.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.service.GoalService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeServiceTests {

    @Test
    void shouldReturnTheGoalDailyTargetBalanceAndNextPendingExpense() {
        GoalService goalService = mock(GoalService.class);
        FinancialEntryRepository entryRepository = mock(FinancialEntryRepository.class);
        HomeService service = new HomeService(goalService, entryRepository);
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
        when(entryRepository.findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.of(nextExpense));

        var response = service.current(userId, date);

        assertEquals(money("182.76"), response.goal().dailyTarget());
        assertEquals(money("-1558.60"), response.financial().balance());
        assertEquals(expenseId, response.nextExpense().id());
        assertEquals(money("329.00"), response.nextExpense().amount());
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
