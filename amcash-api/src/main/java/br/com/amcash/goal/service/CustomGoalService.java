package br.com.amcash.goal.service;

import br.com.amcash.goal.dto.request.CustomGoalRequest;
import br.com.amcash.goal.dto.response.CustomGoalResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomGoalService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CustomGoalRepository customGoalRepository;
    private final GoalMonthPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final WorkService workService;

    public CustomGoalService(CustomGoalRepository customGoalRepository,
                             GoalMonthPreferenceRepository preferenceRepository,
                             UserRepository userRepository,
                             WorkService workService) {
        this.customGoalRepository = customGoalRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.workService = workService;
    }

    @Transactional(readOnly = true)
    public Optional<CustomGoalResponse> current(UUID userId, LocalDate referenceDate) {
        return customGoalRepository.findByUserId(userId)
                .map(goal -> calculate(userId, goal, referenceDate));
    }

    @Transactional(readOnly = true)
    public Optional<CustomGoalResponse> currentForHome(UUID userId, LocalDate referenceDate) {
        return customGoalRepository.findByUserIdAndTrackedOnHomeTrue(userId)
                .map(goal -> calculate(userId, goal, referenceDate));
    }

    @Transactional(readOnly = true)
    public PrimaryGoalType primary(UUID userId) {
        return customGoalRepository.findByUserIdAndTrackedOnHomeTrue(userId)
                .map(ignored -> PrimaryGoalType.CUSTOM)
                .orElse(PrimaryGoalType.EXPENSES);
    }

    @Transactional
    public PrimaryGoalType selectPrimary(UUID userId, PrimaryGoalType type) {
        Optional<CustomGoal> current = customGoalRepository.findByUserId(userId);
        if (type == PrimaryGoalType.CUSTOM) {
            CustomGoal goal = current.orElseThrow(() -> new BadRequestException("Cadastre uma meta personalizada antes de selecioná-la"));
            if (!goal.isTrackedOnHome()) {
                goal.updateTrackedOnHome(true);
                customGoalRepository.save(goal);
            }
            return PrimaryGoalType.CUSTOM;
        }
        current.filter(CustomGoal::isTrackedOnHome).ifPresent(goal -> {
            goal.updateTrackedOnHome(false);
            customGoalRepository.save(goal);
        });
        return PrimaryGoalType.EXPENSES;
    }

    @Transactional
    public CustomGoalResponse create(UUID userId, LocalDate referenceDate, CustomGoalRequest request) {
        if (customGoalRepository.existsByUserId(userId)) {
            throw new BadRequestException("Você já possui uma meta personalizada ativa");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        CustomGoal goal = new CustomGoal(user, request.name().trim(), request.amount(), request.period());
        return calculate(userId, customGoalRepository.save(goal), referenceDate);
    }

    @Transactional
    public CustomGoalResponse update(UUID userId, LocalDate referenceDate, CustomGoalRequest request) {
        CustomGoal goal = customGoalRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Meta personalizada não encontrada"));
        goal.update(request.name().trim(), request.amount(), request.period());
        return calculate(userId, customGoalRepository.save(goal), referenceDate);
    }

    @Transactional
    public void delete(UUID userId) {
        CustomGoal goal = customGoalRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Meta personalizada não encontrada"));
        customGoalRepository.delete(goal);
    }

    private CustomGoalResponse calculate(UUID userId, CustomGoal goal, LocalDate referenceDate) {
        LocalDate start = periodStart(referenceDate, goal.getPeriod());
        LocalDate end = periodEnd(referenceDate, goal.getPeriod());
        LocalDate calculationEnd = referenceDate.isBefore(end) ? referenceDate : end;
        WorkResultProgressSnapshot result = workService.resultProgress(userId, start, calculationEnd, referenceDate);
        BigDecimal remaining = goal.getTargetAmount().subtract(result.net()).max(BigDecimal.ZERO);
        Set<Integer> selectedDays = selectedDays(userId, YearMonth.from(referenceDate));
        boolean todaySelected = selectedDays.contains(referenceDate.getDayOfMonth());
        int availableDays = goal.getPeriod() == CustomGoalPeriod.DAILY
                ? (todaySelected ? 1 : 0)
                : countAvailableDays(referenceDate, end, selectedDays);
        BigDecimal dailyTarget = remaining.signum() == 0 ? BigDecimal.ZERO
                : goal.getPeriod() == CustomGoalPeriod.DAILY || availableDays == 0 ? remaining
                : remaining.divide(BigDecimal.valueOf(availableDays), 2, RoundingMode.HALF_UP);
        BigDecimal resultBeforeToday = result.net().subtract(result.todayNet());
        BigDecimal remainingAtDayStart = goal.getTargetAmount().subtract(resultBeforeToday).max(BigDecimal.ZERO);
        BigDecimal todayTarget = goal.getPeriod() == CustomGoalPeriod.DAILY
                ? goal.getTargetAmount()
                : availableDays == 0 ? remainingAtDayStart
                : remainingAtDayStart.divide(BigDecimal.valueOf(availableDays), 2, RoundingMode.HALF_UP);
        BigDecimal todayRemaining = todayTarget.subtract(result.todayNet()).max(BigDecimal.ZERO);
        BigDecimal progress = result.net().signum() <= 0 ? BigDecimal.ZERO
                : result.net().multiply(ONE_HUNDRED)
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .min(ONE_HUNDRED);

        return new CustomGoalResponse(goal.getId(), goal.getName(), money(goal.getTargetAmount()), goal.getPeriod(),
                referenceDate, start, end, result.gross(), result.costs(), result.net(), money(remaining),
                money(dailyTarget), money(todayTarget), money(todayRemaining), availableDays,
                money(progress), remaining.signum() == 0, todaySelected,
                selectedDays.stream().sorted().toList());
    }

    private Set<Integer> selectedDays(UUID userId, YearMonth month) {
        return preferenceRepository.findByUserIdAndYearAndMonth(userId, month.getYear(), month.getMonthValue())
                .map(preference -> daysFromMask(preference.getSelectedDaysMask(), month.lengthOfMonth()))
                .orElseGet(Set::of);
    }

    private int countAvailableDays(LocalDate referenceDate, LocalDate end, Set<Integer> selectedDays) {
        YearMonth referenceMonth = YearMonth.from(referenceDate);
        int count = 0;
        for (LocalDate date = referenceDate; !date.isAfter(end); date = date.plusDays(1)) {
            if (YearMonth.from(date).equals(referenceMonth) && selectedDays.contains(date.getDayOfMonth())) count++;
        }
        return count;
    }

    private Set<Integer> daysFromMask(int mask, int daysInMonth) {
        Set<Integer> days = new HashSet<>();
        for (int day = 1; day <= daysInMonth; day++) if ((mask & (1 << (day - 1))) != 0) days.add(day);
        return days;
    }

    private LocalDate periodStart(LocalDate date, CustomGoalPeriod period) {
        return switch (period) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> YearMonth.from(date).atDay(1);
        };
    }

    private LocalDate periodEnd(LocalDate date, CustomGoalPeriod period) {
        return switch (period) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6);
            case MONTHLY -> YearMonth.from(date).atEndOfMonth();
        };
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
