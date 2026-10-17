package br.com.amcash.work.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.work.dto.request.UpsertWorkDayRequest;
import br.com.amcash.work.dto.request.WorkCostRequest;
import br.com.amcash.work.dto.response.WorkCostResponse;
import br.com.amcash.work.dto.response.WorkDayResponse;
import br.com.amcash.work.dto.response.WorkOverviewResponse;
import br.com.amcash.work.entity.WorkPeriod;
import br.com.amcash.work.service.WorkService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work")
public class WorkController {

    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    @GetMapping
    public WorkOverviewResponse overview(
            @CurrentUser AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "MONTH") WorkPeriod period) {
        return workService.overview(user.userId(), date, period);
    }

    @PutMapping("/days/{date}")
    public WorkDayResponse upsert(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpsertWorkDayRequest request) {
        return workService.upsert(user.userId(), date, request);
    }

    @DeleteMapping("/days/{date}")
    public ResponseEntity<Void> delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        workService.delete(user.userId(), date);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/costs")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public WorkCostResponse createCost(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody WorkCostRequest request) {
        return workService.createCost(user.userId(), request);
    }

    @PutMapping("/costs/{costId}")
    public WorkCostResponse updateCost(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID costId,
            @Valid @RequestBody WorkCostRequest request) {
        return workService.updateCost(user.userId(), costId, request);
    }

    @DeleteMapping("/costs/{costId}")
    public ResponseEntity<Void> deleteCost(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID costId) {
        workService.deleteCost(user.userId(), costId);
        return ResponseEntity.noContent().build();
    }
}
