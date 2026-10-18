package br.com.amcash.home.service;

import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.service.GoalService;
import br.com.amcash.work.service.WorkService;
import br.com.amcash.work.service.WorkService.WorkProgressSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeServiceTests {

    @Test
    void shouldUseExactlyTheDailyTargetCalculatedByGoals() {
        GoalService goalService = mock(GoalService.class);
        WorkService workService = mock(WorkService.class);
        HomeService service = new HomeService(goalService, workService);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 18);
        GoalResponse goal = new GoalResponse(2026, 10, date,
                money("0"), money("2558.60"), money("0"), money("2558.60"),
                money("2558.60"), money("1279.32"), money("182.76"), 14,
                money("0"), false, true, List.of(18, 19));
        WorkProgressSnapshot work = new WorkProgressSnapshot(
                money("181.89"), money("0"), money("181.89"),
                money("0"), money("0"), money("0"));
        when(goalService.current(userId, date)).thenReturn(goal);
        when(workService.progress(userId, date, YearMonth.from(date))).thenReturn(work);

        var response = service.current(userId, date);

        assertEquals(money("182.76"), response.goal().todayTarget());
        assertEquals(money("182.76"), response.goal().todayRemaining());
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
