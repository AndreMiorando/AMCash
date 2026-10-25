package br.com.amcash.home.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.entity.CustomGoalPeriod;
import br.com.amcash.goal.entity.PrimaryGoalType;
import br.com.amcash.goal.service.GoalService;
import br.com.amcash.home.dto.HomeResponse;
import br.com.amcash.work.service.WorkService;
import br.com.amcash.work.service.WorkService.WorkResultSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class HomeService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final GoalService goalService;
    private final FinancialEntryRepository entryRepository;
    private final WorkService workService;

    public HomeService(GoalService goalService, FinancialEntryRepository entryRepository, WorkService workService) {
        this.goalService = goalService;
        this.entryRepository = entryRepository;
        this.workService = workService;
    }

    @Transactional(readOnly = true)
    public HomeResponse current(UUID userId, LocalDate date) {
        GoalResponse goal = goalService.current(userId, date);
        YearMonth month = YearMonth.from(date);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        boolean hasFinancialEntries = goal.income().signum() != 0 || goal.totalExpenses().signum() != 0;
        HomeResponse.NextExpense nextExpense = entryRepository
                .findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                        userId, EntryType.EXPENSE, monthStart, monthEnd)
                .map(this::toNextExpense)
                .orElse(null);
        long pendingExpenseCount = entryRepository.countByUserIdAndTypeAndPaidFalseAndDueDateBetween(
                userId, EntryType.EXPENSE, monthStart, monthEnd);
        WorkResultSnapshot work = workService.result(userId, date, date);
        BigDecimal baseDailyTarget = money(goal.todayTarget());
        BigDecimal dailyTarget = money(baseDailyTarget.add(work.costs()));
        BigDecimal todayRemaining = money(dailyTarget.subtract(work.gross()).max(BigDecimal.ZERO));
        BigDecimal progress = dailyTarget.signum() == 0 ? money(ONE_HUNDRED)
                : money(work.gross().max(BigDecimal.ZERO).multiply(ONE_HUNDRED)
                    .divide(dailyTarget, 2, RoundingMode.HALF_UP).min(ONE_HUNDRED));
        boolean achieved = work.gross().compareTo(dailyTarget) >= 0;
        HomeResponse.GoalSummary goalSummary = new HomeResponse.GoalSummary(
                PrimaryGoalType.EXPENSES, "Objetivo mensal", CustomGoalPeriod.MONTHLY,
                goal.targetAmount(), dailyTarget, baseDailyTarget, work.gross(), work.costs(),
                todayRemaining, progress, goal.remainingAmount(), achieved, goal.covered(), goal.todaySelected());
        return new HomeResponse(date, hasFinancialEntries,
                new HomeResponse.FinancialSummary(
                        goal.income(), goal.pendingExpenses(), pendingExpenseCount,
                        money(goal.income().subtract(goal.pendingExpenses()))),
                goalSummary,
                nextExpense);
    }

    private HomeResponse.NextExpense toNextExpense(FinancialEntry entry) {
        return new HomeResponse.NextExpense(entry.getId(), entry.getName(), entry.getDueDate(), money(entry.getAmount()));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
