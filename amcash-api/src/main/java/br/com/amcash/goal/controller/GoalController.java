package br.com.amcash.goal.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.service.GoalService;
import br.com.amcash.shared.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public GoalResponse current(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return goalService.current(user.userId(), date, targetMonth(date, year, month));
    }

    @PutMapping("/preferences")
    public GoalResponse updatePreferences(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @Valid @RequestBody UpdateGoalPreferencesRequest request) {
        return goalService.updatePreferences(user.userId(), date, targetMonth(date, year, month), request);
    }

    private YearMonth targetMonth(LocalDate referenceDate, Integer year, Integer month) {
        if (year == null && month == null) return YearMonth.from(referenceDate);
        if (year == null || month == null) {
            throw new BadRequestException("Informe o ano e o mês da meta");
        }
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BadRequestException("Ano ou mês da meta inválido");
        }
    }
}
