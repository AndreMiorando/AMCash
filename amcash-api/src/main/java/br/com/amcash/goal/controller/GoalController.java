package br.com.amcash.goal.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.dto.request.UpdateGoalConfigurationRequest;
import br.com.amcash.goal.dto.request.CustomGoalRequest;
import br.com.amcash.goal.dto.request.UpdatePrimaryGoalRequest;
import br.com.amcash.goal.dto.response.CustomGoalResponse;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.dto.response.PrimaryGoalResponse;
import br.com.amcash.goal.service.CustomGoalService;
import br.com.amcash.goal.service.GoalService;
import br.com.amcash.shared.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;
    private final CustomGoalService customGoalService;

    public GoalController(GoalService goalService, CustomGoalService customGoalService) {
        this.goalService = goalService;
        this.customGoalService = customGoalService;
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

    @PutMapping("/configuration")
    public GoalResponse updateConfiguration(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpdateGoalConfigurationRequest request) {
        return goalService.updateConfiguration(user.userId(), date, request);
    }

    @GetMapping("/custom")
    public ResponseEntity<CustomGoalResponse> custom(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return customGoalService.current(user.userId(), date)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/custom")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public CustomGoalResponse createCustom(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CustomGoalRequest request) {
        return customGoalService.create(user.userId(), date, request);
    }

    @PutMapping("/custom")
    public CustomGoalResponse updateCustom(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CustomGoalRequest request) {
        return customGoalService.update(user.userId(), date, request);
    }

    @DeleteMapping("/custom")
    public ResponseEntity<Void> deleteCustom(@CurrentUser AuthenticatedUser user) {
        customGoalService.delete(user.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/primary")
    public PrimaryGoalResponse primary(@CurrentUser AuthenticatedUser user) {
        return new PrimaryGoalResponse(customGoalService.primary(user.userId()));
    }

    @PutMapping("/primary")
    public PrimaryGoalResponse updatePrimary(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody UpdatePrimaryGoalRequest request) {
        return new PrimaryGoalResponse(customGoalService.selectPrimary(user.userId(), request.type()));
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
