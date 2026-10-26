package br.com.amcash.work.service;

import br.com.amcash.operationalcost.dto.response.OperationalCostResponse;
import br.com.amcash.operationalcost.entity.CostPeriod;
import br.com.amcash.operationalcost.entity.MaintenanceCalculationMethod;
import br.com.amcash.operationalcost.entity.OperationalCostConfiguration;
import br.com.amcash.operationalcost.entity.OperationalCustomFixedCost;
import br.com.amcash.operationalcost.repository.OperationalCostConfigurationRepository;
import br.com.amcash.operationalcost.service.OperationalCostService;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.vehicle.entity.Vehicle;
import br.com.amcash.work.entity.WorkPeriod;
import br.com.amcash.work.entity.WorkCost;
import br.com.amcash.work.entity.WorkCostCategory;
import br.com.amcash.work.entity.WorkDay;
import br.com.amcash.work.entity.WorkEarning;
import br.com.amcash.work.repository.WorkCostRepository;
import br.com.amcash.work.repository.WorkDayRepository;
import br.com.amcash.work.repository.WorkEarningRepository;
import br.com.amcash.vehicle.repository.VehicleRepository;
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
    void shouldEstimateEachVehicleWithoutDiscountingCoveredCostsTwice() {
        WorkDayRepository dayRepository = mock(WorkDayRepository.class);
        WorkEarningRepository earningRepository = mock(WorkEarningRepository.class);
        WorkCostRepository costRepository = mock(WorkCostRepository.class);
        OperationalCostConfigurationRepository configurationRepository = mock(OperationalCostConfigurationRepository.class);
        OperationalCostService operationalCostService = mock(OperationalCostService.class);
        WorkService service = new WorkService(dayRepository, earningRepository, costRepository,
                mock(UserRepository.class), mock(VehicleRepository.class), configurationRepository, operationalCostService);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 19);
        Vehicle vehicle = new Vehicle(mock(User.class));
        Vehicle secondVehicle = new Vehicle(mock(User.class));
        WorkDay day = mock(WorkDay.class);
        WorkDay secondDay = mock(WorkDay.class);
        UUID dayId = UUID.randomUUID();
        UUID secondDayId = UUID.randomUUID();
        when(day.getId()).thenReturn(dayId);
        when(day.getDate()).thenReturn(date);
        when(day.getVehicle()).thenReturn(vehicle);
        when(day.getKilometers()).thenReturn(new BigDecimal("100"));
        when(secondDay.getId()).thenReturn(secondDayId);
        when(secondDay.getDate()).thenReturn(date.plusDays(1));
        when(secondDay.getVehicle()).thenReturn(secondVehicle);
        when(secondDay.getKilometers()).thenReturn(new BigDecimal("50"));
        WorkEarning earning = mock(WorkEarning.class);
        when(earning.getWorkDay()).thenReturn(day);
        when(earning.getAmount()).thenReturn(new BigDecimal("300"));
        when(earning.getSource()).thenReturn("Uber");
        OperationalCostConfiguration configuration = mock(OperationalCostConfiguration.class);
        OperationalCostConfiguration secondConfiguration = mock(OperationalCostConfiguration.class);
        when(configuration.getVehicle()).thenReturn(vehicle);
        when(configuration.getMaintenanceMethod()).thenReturn(MaintenanceCalculationMethod.MANUAL);
        when(configuration.getAnnualMaintenanceEstimate()).thenReturn(new BigDecimal("1200"));
        when(configuration.getCustomFixedCosts()).thenReturn(List.of(
                new OperationalCustomFixedCost("Lavagem mensal", new BigDecimal("120"), CostPeriod.MONTHLY, WorkCostCategory.WASH)));
        when(secondConfiguration.getVehicle()).thenReturn(secondVehicle);
        when(secondConfiguration.getMaintenanceMethod()).thenReturn(MaintenanceCalculationMethod.LAST_12_MONTHS);
        when(secondConfiguration.getCustomFixedCosts()).thenReturn(List.of());
        when(configurationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(configuration, secondConfiguration));
        when(operationalCostService.calculation(configuration)).thenReturn(new OperationalCostResponse.CalculationResponse(
                money("0"), money("0"), money("0"), money("0"), money("0"), money("0"),
                money("0"), new BigDecimal("1.4000"), money("0"), money("0"), money("0")));
        when(operationalCostService.calculation(secondConfiguration)).thenReturn(new OperationalCostResponse.CalculationResponse(
                money("0"), money("0"), money("0"), money("0"), money("0"), money("0"),
                money("0"), new BigDecimal("2.0000"), money("0"), money("0"), money("0")));
        when(dayRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(userId, date, date.plusDays(6))).thenReturn(List.of(day, secondDay));
        when(earningRepository.findAllByWorkDayIdInOrderByWorkDayIdAscSortOrderAsc(List.of(dayId, secondDayId))).thenReturn(List.of(earning));
        List<WorkCost> costs = List.of(cost(date, WorkCostCategory.FUEL, "100"),
                cost(date, WorkCostCategory.WASH, "30"), cost(date, WorkCostCategory.FOOD, "25"));
        when(costRepository.findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(userId, date, date.plusDays(6)))
                .thenReturn(costs);
        when(earningRepository.sumByUserAndDateBetween(userId, date.minusDays(7), date.minusDays(1))).thenReturn(BigDecimal.ZERO);
        when(costRepository.sumByUserAndDateBetween(userId, date.minusDays(7), date.minusDays(1))).thenReturn(BigDecimal.ZERO);

        var estimated = service.overview(userId, date, WorkPeriod.WEEK).estimatedSummary();

        assertEquals(true, estimated.available());
        assertEquals(money("240"), estimated.operationalCost());
        assertEquals(money("25"), estimated.additionalCosts());
        assertEquals(money("265"), estimated.totalCosts());
        assertEquals(money("35"), estimated.netEarnings());
    }

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
                mock(UserRepository.class),mock(VehicleRepository.class),
                mock(br.com.amcash.operationalcost.repository.OperationalCostConfigurationRepository.class),
                mock(br.com.amcash.operationalcost.service.OperationalCostService.class));
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
                mock(UserRepository.class),mock(VehicleRepository.class),
                mock(br.com.amcash.operationalcost.repository.OperationalCostConfigurationRepository.class),
                mock(br.com.amcash.operationalcost.service.OperationalCostService.class));
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

    private WorkCost cost(LocalDate date, WorkCostCategory category, String amount) {
        WorkCost cost = mock(WorkCost.class);
        when(cost.getId()).thenReturn(UUID.randomUUID());
        when(cost.getDate()).thenReturn(date);
        when(cost.getCategory()).thenReturn(category);
        when(cost.getAmount()).thenReturn(new BigDecimal(amount));
        return cost;
    }
}
