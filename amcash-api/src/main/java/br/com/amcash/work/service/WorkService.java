package br.com.amcash.work.service;

import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.dto.request.UpsertWorkDayRequest;
import br.com.amcash.work.dto.request.WorkCostRequest;
import br.com.amcash.work.dto.request.WorkEarningRequest;
import br.com.amcash.work.dto.response.WorkCostResponse;
import br.com.amcash.work.dto.response.WorkComparisonResponse;
import br.com.amcash.work.dto.response.WorkDayResponse;
import br.com.amcash.work.dto.response.WorkEarningResponse;
import br.com.amcash.work.dto.response.WorkOverviewResponse;
import br.com.amcash.work.dto.response.WorkSummaryResponse;
import br.com.amcash.work.entity.WorkCost;
import br.com.amcash.work.entity.WorkCostCategory;
import br.com.amcash.work.entity.WorkDay;
import br.com.amcash.work.entity.WorkEarning;
import br.com.amcash.work.entity.WorkPeriod;
import br.com.amcash.work.repository.WorkCostRepository;
import br.com.amcash.work.repository.WorkDayRepository;
import br.com.amcash.work.repository.WorkEarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final WorkDayRepository workDayRepository;
    private final WorkEarningRepository workEarningRepository;
    private final WorkCostRepository workCostRepository;
    private final UserRepository userRepository;

    public WorkService(WorkDayRepository workDayRepository, WorkEarningRepository workEarningRepository,
                       WorkCostRepository workCostRepository, UserRepository userRepository) {
        this.workDayRepository = workDayRepository;
        this.workEarningRepository = workEarningRepository;
        this.workCostRepository = workCostRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WorkOverviewResponse overview(UUID userId, LocalDate referenceDate, WorkPeriod period) {
        LocalDate start = rangeStart(referenceDate, period);
        LocalDate end = rangeEnd(referenceDate, period);
        List<WorkDay> days = workDayRepository.findAllByUserIdAndDateBetweenOrderByDateDesc(userId, start, end);
        Map<UUID, List<WorkEarning>> earningsByDay = loadEarnings(days);
        List<WorkDayResponse> dayResponses = days.stream()
                .map(day -> toResponse(day, earningsByDay.getOrDefault(day.getId(), List.of())))
                .toList();
        List<WorkCostResponse> costResponses = workCostRepository
                .findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(userId, start, end)
                .stream().map(this::toResponse).toList();
        WorkSummaryResponse summary = summarize(dayResponses, costResponses);
        return new WorkOverviewResponse(period.name(), referenceDate, start, end,
                summary, comparison(userId, referenceDate, period, summary), dayResponses, costResponses);
    }

    @Transactional(readOnly = true)
    public WorkProgressSnapshot progress(UUID userId, LocalDate referenceDate, YearMonth targetMonth) {
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();
        LocalDate aggregationDate = targetMonth.equals(YearMonth.from(referenceDate))
                ? referenceDate : start.minusDays(1);
        BigDecimal[] earnings = summary(workEarningRepository
                .summarizeMonth(userId, start, end, aggregationDate));
        BigDecimal[] costs = summary(workCostRepository
                .summarizeMonth(userId, start, end, aggregationDate));
        return new WorkProgressSnapshot(
                money(earnings[0]), money(costs[0]), money(earnings[0].subtract(costs[0])),
                money(earnings[1]), money(costs[1]), money(earnings[1].subtract(costs[1])));
    }

    @Transactional(readOnly = true)
    public WorkResultSnapshot result(UUID userId, LocalDate start, LocalDate end) {
        BigDecimal gross = workEarningRepository.sumByUserAndDateBetween(userId, start, end);
        BigDecimal costs = workCostRepository.sumByUserAndDateBetween(userId, start, end);
        return new WorkResultSnapshot(money(gross), money(costs), money(gross.subtract(costs)));
    }

    @Transactional(readOnly = true)
    public WorkResultProgressSnapshot resultProgress(UUID userId, LocalDate start, LocalDate end,
                                                     LocalDate referenceDate) {
        BigDecimal[] earnings = summary(workEarningRepository.summarizeMonth(userId, start, end, referenceDate));
        BigDecimal[] costs = summary(workCostRepository.summarizeMonth(userId, start, end, referenceDate));
        return new WorkResultProgressSnapshot(
                money(earnings[0]), money(costs[0]), money(earnings[0].subtract(costs[0])),
                money(earnings[1]), money(costs[1]), money(earnings[1].subtract(costs[1])));
    }

    @Transactional
    public WorkDayResponse upsert(UUID userId, LocalDate date, UpsertWorkDayRequest request) {
        validateEarnings(request.earnings());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        WorkDay day = workDayRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> new WorkDay(user, date, request.kilometers()));
        day.update(request.kilometers());
        day = workDayRepository.save(day);
        workEarningRepository.deleteAllByWorkDayId(day.getId());
        List<WorkEarning> earnings = new ArrayList<>();
        for (int index = 0; index < request.earnings().size(); index++) {
            WorkEarningRequest earning = request.earnings().get(index);
            earnings.add(new WorkEarning(day, earning.source().trim(), earning.amount(), index));
        }
        if (!earnings.isEmpty()) workEarningRepository.saveAll(earnings);
        return toResponse(day, earnings);
    }

    @Transactional
    public void delete(UUID userId, LocalDate date) {
        WorkDay day = workDayRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new NotFoundException("Registro de trabalho não encontrado"));
        workDayRepository.delete(day);
    }

    @Transactional
    public WorkCostResponse createCost(UUID userId, WorkCostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        WorkCost cost = new WorkCost(user, request.date(), request.category(),
                normalize(request.description()), request.amount());
        return toResponse(workCostRepository.save(cost));
    }

    @Transactional
    public WorkCostResponse updateCost(UUID userId, UUID costId, WorkCostRequest request) {
        WorkCost cost = workCostRepository.findByIdAndUserId(costId, userId)
                .orElseThrow(() -> new NotFoundException("Custo não encontrado"));
        cost.update(request.date(), request.category(), normalize(request.description()), request.amount());
        return toResponse(workCostRepository.save(cost));
    }

    @Transactional
    public void deleteCost(UUID userId, UUID costId) {
        WorkCost cost = workCostRepository.findByIdAndUserId(costId, userId)
                .orElseThrow(() -> new NotFoundException("Custo não encontrado"));
        workCostRepository.delete(cost);
    }

    private Map<UUID, List<WorkEarning>> loadEarnings(List<WorkDay> days) {
        if (days.isEmpty()) return Map.of();
        Map<UUID, List<WorkEarning>> earningsByDay = new HashMap<>();
        List<UUID> dayIds = days.stream().map(WorkDay::getId).toList();
        for (WorkEarning earning : workEarningRepository.findAllByWorkDayIdInOrderByWorkDayIdAscSortOrderAsc(dayIds)) {
            earningsByDay.computeIfAbsent(earning.getWorkDay().getId(), ignored -> new ArrayList<>()).add(earning);
        }
        return earningsByDay;
    }

    private WorkDayResponse toResponse(WorkDay day, List<WorkEarning> earnings) {
        BigDecimal gross = earnings.stream().map(WorkEarning::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal perKilometer = day.getKilometers().signum() == 0
                ? ZERO : gross.divide(day.getKilometers(), 2, RoundingMode.HALF_UP);
        List<WorkEarningResponse> earningResponses = earnings.stream()
                .map(earning -> new WorkEarningResponse(earning.getId(), earning.getSource(), earning.getAmount()))
                .toList();
        return new WorkDayResponse(day.getId(), day.getDate(), day.getKilometers(), money(gross),
                perKilometer, earningResponses);
    }

    private WorkCostResponse toResponse(WorkCost cost) {
        return new WorkCostResponse(cost.getId(), cost.getDate(), cost.getCategory(),
                cost.getDescription(), money(cost.getAmount()));
    }

    private WorkSummaryResponse summarize(List<WorkDayResponse> days, List<WorkCostResponse> costEntries) {
        BigDecimal gross = sum(days, WorkDayResponse::grossEarnings);
        BigDecimal fuel = costEntries.stream().filter(cost -> cost.category() == WorkCostCategory.FUEL)
                .map(WorkCostResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal other = costEntries.stream().filter(cost -> cost.category() != WorkCostCategory.FUEL)
                .map(WorkCostResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal kilometers = sum(days, WorkDayResponse::kilometers);
        BigDecimal costs = fuel.add(other);
        BigDecimal perKilometer = kilometers.signum() == 0
                ? ZERO : gross.divide(kilometers, 2, RoundingMode.HALF_UP);
        return new WorkSummaryResponse(money(gross), money(fuel), money(other), money(costs),
                money(gross.subtract(costs)), kilometers.setScale(2, RoundingMode.HALF_UP),
                perKilometer, days.size());
    }

    private WorkComparisonResponse comparison(UUID userId, LocalDate referenceDate, WorkPeriod period,
                                              WorkSummaryResponse currentSummary) {
        LocalDate previousReference = switch (period) {
            case DAY -> referenceDate.minusDays(1);
            case WEEK -> rangeStart(referenceDate, period).minusWeeks(1);
            case MONTH -> YearMonth.from(referenceDate).minusMonths(1).atDay(1);
        };
        LocalDate previousStart = rangeStart(previousReference, period);
        LocalDate previousEnd = rangeEnd(previousReference, period);
        BigDecimal previousGross = workEarningRepository.sumByUserAndDateBetween(userId, previousStart, previousEnd);
        BigDecimal previousCosts = workCostRepository.sumByUserAndDateBetween(userId, previousStart, previousEnd);
        BigDecimal previousNet = money(previousGross.subtract(previousCosts));
        BigDecimal changeAmount = money(currentSummary.netEarnings().subtract(previousNet));
        BigDecimal changePercentage = previousNet.signum() == 0 ? null : changeAmount
                .divide(previousNet.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        return new WorkComparisonResponse(previousStart, previousEnd, previousNet, changeAmount, changePercentage);
    }

    private BigDecimal sum(List<WorkDayResponse> days,
                           java.util.function.Function<WorkDayResponse, BigDecimal> value) {
        return days.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateEarnings(List<WorkEarningRequest> earnings) {
        Set<String> sources = new HashSet<>();
        for (WorkEarningRequest earning : earnings) {
            String normalized = earning.source().trim().toLowerCase(Locale.ROOT);
            if (!sources.add(normalized)) {
                throw new BadRequestException("Cada fonte de faturamento deve aparecer apenas uma vez");
            }
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private LocalDate rangeStart(LocalDate date, WorkPeriod period) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> YearMonth.from(date).atDay(1);
        };
    }

    private LocalDate rangeEnd(LocalDate date, WorkPeriod period) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6);
            case MONTH -> YearMonth.from(date).atEndOfMonth();
        };
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal[] summary(List<Object[]> result) {
        if (result.isEmpty()) return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        Object[] row = result.getFirst();
        return new BigDecimal[]{decimal(row[0]), decimal(row[1])};
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    public record WorkProgressSnapshot(
            BigDecimal monthGross,
            BigDecimal monthCosts,
            BigDecimal monthNet,
            BigDecimal todayGross,
            BigDecimal todayCosts,
            BigDecimal todayNet) {
    }

    public record WorkResultSnapshot(BigDecimal gross, BigDecimal costs, BigDecimal net) {
    }

    public record WorkResultProgressSnapshot(
            BigDecimal gross, BigDecimal costs, BigDecimal net,
            BigDecimal todayGross, BigDecimal todayCosts, BigDecimal todayNet) {
    }
}
