package br.com.amcash.home.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.goal.dto.response.GoalResponse;
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
    private final FinancialEntryRepository entryRepository;

    public HomeService(GoalService goalService, FinancialEntryRepository entryRepository) {
        this.goalService = goalService;
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public HomeResponse current(UUID userId, LocalDate date) {
        GoalResponse goal = goalService.current(userId, date);
        YearMonth month = YearMonth.from(date);
        HomeResponse.NextExpense nextExpense = entryRepository
                .findFirstByUserIdAndTypeAndPaidFalseAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                        userId, EntryType.EXPENSE, month.atDay(1), month.atEndOfMonth())
                .map(this::toNextExpense)
                .orElse(null);
        return new HomeResponse(date,
                new HomeResponse.FinancialSummary(
                        goal.income(), goal.pendingExpenses(), money(goal.income().subtract(goal.pendingExpenses()))),
                new HomeResponse.GoalSummary(goal.dailyTarget(), goal.covered(), goal.todaySelected()),
                nextExpense);
    }

    private HomeResponse.NextExpense toNextExpense(FinancialEntry entry) {
        return new HomeResponse.NextExpense(entry.getId(), entry.getName(), entry.getDueDate(), money(entry.getAmount()));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
