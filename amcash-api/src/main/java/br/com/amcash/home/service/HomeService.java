package br.com.amcash.home.service;

import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.service.GoalService;
import br.com.amcash.home.dto.HomeResponse;
import br.com.amcash.work.service.WorkService;
import br.com.amcash.work.service.WorkService.WorkProgressSnapshot;
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
    private final WorkService workService;

    public HomeService(GoalService goalService, WorkService workService) {
        this.goalService = goalService;
        this.workService = workService;
    }

    @Transactional(readOnly = true)
    public HomeResponse current(UUID userId, LocalDate date) {
        GoalResponse goal = goalService.current(userId, date);
        WorkProgressSnapshot work = workService.progress(userId, date, YearMonth.from(date));
        BigDecimal todayTarget = goal.todaySelected()
                ? goal.dailyTarget() : money(BigDecimal.ZERO);
        BigDecimal achieved = work.todayNet();
        BigDecimal remaining = goal.todaySelected()
                ? todayTarget.subtract(achieved).max(BigDecimal.ZERO) : money(BigDecimal.ZERO);
        BigDecimal surplus = goal.todaySelected()
                ? achieved.subtract(todayTarget).max(BigDecimal.ZERO) : money(BigDecimal.ZERO);
        BigDecimal monthRemaining = goal.remainingAmount().subtract(work.monthNet()).max(BigDecimal.ZERO);
        BigDecimal resources = goal.income().add(work.monthNet()).max(BigDecimal.ZERO);
        BigDecimal progress = goal.pendingExpenses().signum() == 0 ? ONE_HUNDRED
                : resources.multiply(ONE_HUNDRED)
                    .divide(goal.pendingExpenses(), 2, RoundingMode.HALF_UP).min(ONE_HUNDRED);
        return new HomeResponse(date,
                new HomeResponse.FinancialSummary(goal.income(), goal.totalExpenses(),
                        goal.paidExpenses(), goal.pendingExpenses()),
                new HomeResponse.WorkSummary(work.todayGross(), work.todayCosts(), work.todayNet()),
                new HomeResponse.WorkSummary(work.monthGross(), work.monthCosts(), work.monthNet()),
                new HomeResponse.GoalSummary(money(monthRemaining),
                        todayTarget, achieved, money(remaining),
                        money(surplus), progress, goal.availableDays(), monthRemaining.signum() == 0,
                        goal.todaySelected()));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

}
