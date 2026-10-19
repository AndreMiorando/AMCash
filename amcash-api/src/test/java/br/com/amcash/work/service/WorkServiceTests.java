package br.com.amcash.work.service;

import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.entity.WorkPeriod;
import br.com.amcash.work.repository.WorkCostRepository;
import br.com.amcash.work.repository.WorkDayRepository;
import br.com.amcash.work.repository.WorkEarningRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkServiceTests {

    @Test
    void shouldCompareWithTheEquivalentPreviousPeriod() {
        assertPreviousPeriod(WorkPeriod.DAY, LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18));
        assertPreviousPeriod(WorkPeriod.WEEK, LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));
        assertPreviousPeriod(WorkPeriod.MONTH, LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    }

    @Test
    void shouldNotInventAPercentageWhenThePreviousNetResultIsZero() {
        WorkDayRepository dayRepository = mock(WorkDayRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        WorkCostRepository costRepository = mock(WorkCostRepository.class);
        WorkService service = new WorkService(dayRepository, earningRepository, costRepository,
                mock(UserRepository.class));
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 19);
        when(dayRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(userId, date, date)).thenReturn(List.of());
        when(costRepository.findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(userId, date, date))
                .thenReturn(List.of());
        when(earningRepository.sumByUserAndDateBetween(userId, date.minusDays(1), date.minusDays(1)))
                .thenReturn(money("100"));
        when(costRepository.sumByUserAndDateBetween(userId, date.minusDays(1), date.minusDays(1)))
                .thenReturn(money("100"));

        var comparison = service.overview(userId, date, WorkPeriod.DAY).comparison();

        assertEquals(money("0"), comparison.netEarnings());
        assertNull(comparison.changePercentage());
    }

    private void assertPreviousPeriod(WorkPeriod period, LocalDate referenceDate,
                                      LocalDate expectedStart, LocalDate expectedEnd) {
        WorkDayRepository dayRepository = mock(WorkDayRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        WorkCostRepository costRepository = mock(WorkCostRepository.class);
        WorkService service = new WorkService(dayRepository, earningRepository, costRepository,
                mock(UserRepository.class));
        UUID userId = UUID.randomUUID();
        LocalDate currentStart = period == WorkPeriod.DAY ? referenceDate
                : period == WorkPeriod.WEEK ? LocalDate.of(2026, 10, 19) : LocalDate.of(2026, 10, 1);
        LocalDate currentEnd = period == WorkPeriod.DAY ? referenceDate
                : period == WorkPeriod.WEEK ? LocalDate.of(2026, 10, 25) : LocalDate.of(2026, 10, 31);
        when(dayRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(userId, currentStart, currentEnd))
                .thenReturn(List.of());
        when(costRepository.findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(
                userId, currentStart, currentEnd)).thenReturn(List.of());
        when(earningRepository.sumByUserAndDateBetween(userId, expectedStart, expectedEnd))
                .thenReturn(money("100"));
        when(costRepository.sumByUserAndDateBetween(userId, expectedStart, expectedEnd))
                .thenReturn(money("20"));

        var comparison = service.overview(userId, referenceDate, period).comparison();

        assertEquals(expectedStart, comparison.rangeStart());
        assertEquals(expectedEnd, comparison.rangeEnd());
        assertEquals(money("80"), comparison.netEarnings());
        assertEquals(money("-80"), comparison.changeAmount());
        assertEquals(new BigDecimal("-100.0"), comparison.changePercentage());
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
