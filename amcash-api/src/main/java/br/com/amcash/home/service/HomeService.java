package br.com.amcash.home.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.dto.response.CustomGoalResponse;
import br.com.amcash.goal.entity.PrimaryGoalType;
import br.com.amcash.goal.service.CustomGoalService;
import br.com.amcash.goal.service.GoalService;
import br.com.amcash.home.dto.HomeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class HomeService {
    private final GoalService goalService;
    private final CustomGoalService customGoalService;
    private final FinancialEntryRepository entryRepository;

    public HomeService(GoalService goalService, CustomGoalService customGoalService,
                       FinancialEntryRepository entryRepository) {
        this.goalService = goalService;
        this.customGoalService = customGoalService;
        this.entryRepository = entryRepository;
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
        HomeResponse.GoalSummary goalSummary = customGoalService.currentForHome(userId, date)
                .map(this::toGoalSummary)
                .orElseGet(() -> new HomeResponse.GoalSummary(
                        PrimaryGoalType.EXPENSES, "Despesas pendentes", null,
                        goal.dailyTarget(), goal.remainingAmount(), goal.covered(), goal.todaySelected()));
        return new HomeResponse(date, hasFinancialEntries,
                new HomeResponse.FinancialSummary(
                        goal.income(), goal.pendingExpenses(), pendingExpenseCount,
                        money(goal.income().subtract(goal.pendingExpenses()))),
                goalSummary,
                nextExpense);
    }

    private HomeResponse.GoalSummary toGoalSummary(CustomGoalResponse goal) {
        return new HomeResponse.GoalSummary(
                PrimaryGoalType.CUSTOM, goal.name(), goal.period(), goal.todayTarget(),
                goal.remainingAmount(), goal.achieved(), goal.todaySelected());
    }

    private HomeResponse.NextExpense toNextExpense(FinancialEntry entry) {
        return new HomeResponse.NextExpense(entry.getId(), entry.getName(), entry.getDueDate(), money(entry.getAmount()));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
