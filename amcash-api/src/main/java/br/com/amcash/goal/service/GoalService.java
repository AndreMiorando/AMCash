package br.com.amcash.goal.service;

import br.com.amcash.entry.service.MonthlyFinancialSnapshotService;
import br.com.amcash.entry.service.MonthlyFinancialSnapshotService.MonthlyFinancialSnapshot;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.dto.request.UpdateGoalConfigurationRequest;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.entity.GoalMonthPreference;
import br.com.amcash.goal.repository.GoalMonthPreferenceRepository;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import br.com.amcash.work.service.WorkService;
import br.com.amcash.work.service.WorkService.WorkProgressSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class GoalService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final MonthlyFinancialSnapshotService financialSnapshotService;
    private final UserRepository userRepository;
    private final GoalMonthPreferenceRepository goalPreferenceRepository;
    private final WorkService workService;

    public GoalService(MonthlyFinancialSnapshotService financialSnapshotService,
                       UserRepository userRepository, GoalMonthPreferenceRepository goalPreferenceRepository,
                       WorkService workService) {
        this.financialSnapshotService = financialSnapshotService;
        this.userRepository = userRepository;
        this.goalPreferenceRepository = goalPreferenceRepository;
        this.workService = workService;
    }

    @Transactional(readOnly = true)
    public GoalResponse current(UUID userId, LocalDate referenceDate) {
        return current(userId, referenceDate, YearMonth.from(referenceDate));
    }

    @Transactional(readOnly = true)
    public GoalResponse current(UUID userId, LocalDate referenceDate, YearMonth targetMonth) {
        User user = findUser(userId);
        Set<Integer> selectedDays = goalPreferenceRepository
                .findByUserIdAndYearAndMonth(userId, targetMonth.getYear(), targetMonth.getMonthValue())
                .map(preference -> daysFromMask(preference.getSelectedDaysMask(), targetMonth.lengthOfMonth()))
                .orElseGet(Set::of);
        return calculate(user.getId(), referenceDate, targetMonth, selectedDays);
    }

    @Transactional
    public GoalResponse updatePreferences(UUID userId, LocalDate referenceDate,
                                          UpdateGoalPreferencesRequest request) {
        return updatePreferences(userId, referenceDate, YearMonth.from(referenceDate), request);
    }

    @Transactional
    public GoalResponse updatePreferences(UUID userId, LocalDate referenceDate, YearMonth targetMonth,
                                          UpdateGoalPreferencesRequest request) {
        if (request.selectedDays() == null) throw new BadRequestException("Informe os dias disponíveis");
        if (request.selectedDays().stream().anyMatch(day -> day == null || day < 1 || day > targetMonth.lengthOfMonth())) {
            throw new BadRequestException("Existe um dia inválido para o mês selecionado");
        }
        User user = findUser(userId);
        GoalMonthPreference preference = goalPreferenceRepository
                .findByUserIdAndYearAndMonth(userId, targetMonth.getYear(), targetMonth.getMonthValue())
                .orElseGet(() -> new GoalMonthPreference(user, targetMonth.getYear(), targetMonth.getMonthValue(),
                        GoalMonthPreference.ALL_WEEKDAYS_MASK));
        preference.updateSelectedDaysMask(daysToMask(request.selectedDays()));
        goalPreferenceRepository.save(preference);
        return calculate(user.getId(), referenceDate, targetMonth, request.selectedDays());
    }

    @Transactional
    public GoalResponse updateConfiguration(UUID userId, LocalDate referenceDate,
                                            UpdateGoalConfigurationRequest request) {
        // Mantém o contrato legado sem permitir que configurações personalizadas
        // alterem o objetivo, cuja fonte de verdade agora é exclusivamente Finanças.
        return current(userId, referenceDate);
    }

    private GoalResponse calculate(UUID userId, LocalDate referenceDate, YearMonth month, Set<Integer> selectedDays) {
        MonthlyFinancialSnapshot financial = financialSnapshotService.load(userId, month);
        BigDecimal targetAmount = financial.pendingExpenses();
        BigDecimal remaining = targetAmount.subtract(financial.income()).max(BigDecimal.ZERO);
        WorkProgressSnapshot work = workService.progress(userId, referenceDate, month);
        boolean currentMonth = month.equals(YearMonth.from(referenceDate));
        boolean todaySelected = currentMonth && selectedDays.contains(referenceDate.getDayOfMonth());

        int availableDays = countRemainingSelectedDays(referenceDate, month, selectedDays);
        BigDecimal dailyTarget = remaining.signum() == 0 || availableDays == 0 ? money(BigDecimal.ZERO)
                : money(remaining.divide(BigDecimal.valueOf(availableDays), 2, RoundingMode.HALF_UP));
        BigDecimal weeklyTarget = money(dailyTarget
                .multiply(BigDecimal.valueOf(Math.max(1, Math.min(availableDays, 7))))
                .min(remaining));
        BigDecimal todayTarget = dailyTarget;
        BigDecimal todayRemaining = todayTarget.subtract(work.todayNet()).max(BigDecimal.ZERO);
        BigDecimal progress = targetAmount.signum() == 0 ? ONE_HUNDRED
                : financial.income().max(BigDecimal.ZERO).multiply(ONE_HUNDRED)
                    .divide(targetAmount, 2, RoundingMode.HALF_UP).min(ONE_HUNDRED);

        return new GoalResponse(month.getYear(), month.getMonthValue(), referenceDate,
                money(financial.income()), money(financial.totalExpenses()), money(financial.paidExpenses()),
                money(financial.pendingExpenses()), true, money(financial.pendingExpenses()),
                money(BigDecimal.ZERO), money(targetAmount), money(financial.income()), money(remaining),
                weeklyTarget, dailyTarget, money(todayTarget), money(todayRemaining),
                availableDays, progress, remaining.signum() == 0, todaySelected,
                selectedDays.stream().sorted().toList());
    }

    private int countRemainingSelectedDays(LocalDate referenceDate, YearMonth month, Set<Integer> selectedDays) {
        return (int) selectedDays.stream().map(month::atDay).filter(date -> !date.isBefore(referenceDate)).count();
    }

    private int daysToMask(Set<Integer> days) {
        return days.stream().mapToInt(day -> 1 << (day - 1)).reduce(0, (mask, day) -> mask | day);
    }

    private Set<Integer> daysFromMask(int mask, int daysInMonth) {
        Set<Integer> days = new HashSet<>();
        for (int day = 1; day <= daysInMonth; day++) if ((mask & (1 << (day - 1))) != 0) days.add(day);
        return days;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
