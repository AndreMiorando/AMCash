package br.com.amcash.entry.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.entry.dto.request.CreateEntryRequest;
import br.com.amcash.entry.dto.request.SubexpenseRequest;
import br.com.amcash.entry.dto.request.UpdateEntryRequest;
import br.com.amcash.entry.dto.response.CreatedEntriesResponse;
import br.com.amcash.entry.dto.response.EntryResponse;
import br.com.amcash.entry.dto.response.ForecastResponse;
import br.com.amcash.entry.dto.response.MonthlyEntriesResponse;
import br.com.amcash.entry.dto.response.SubexpenseResponse;
import br.com.amcash.entry.entity.SeriesScope;
import br.com.amcash.entry.service.EntryService;
import br.com.amcash.notification.service.FinancialNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class EntryController {

    private final EntryService entryService;
    private final FinancialNotificationService notificationService;

    public EntryController(EntryService entryService, FinancialNotificationService notificationService) {
        this.entryService = entryService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public MonthlyEntriesResponse listMonth(
            @CurrentUser AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam int month) {

        return entryService.listMonth(user.userId(), year, month);
    }

    @GetMapping("/forecast")
    public ForecastResponse forecast(
            @CurrentUser AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "6") int months) {

        return entryService.forecast(user.userId(), year, month, months);
    }

    @PostMapping
    public ResponseEntity<CreatedEntriesResponse> create(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateEntryRequest request) {

        CreatedEntriesResponse created = entryService.create(user.userId(), request);
        notificationService.synchronize(user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{entryId}")
    public EntryResponse get(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId) {

        return entryService.get(user.userId(), entryId);
    }

    @PutMapping("/{entryId}")
    public EntryResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @RequestParam(defaultValue = "CURRENT") SeriesScope scope,
            @Valid @RequestBody UpdateEntryRequest request) {

        EntryResponse updated = entryService.update(user.userId(), entryId, request, scope);
        notificationService.synchronize(user.userId());
        return updated;
    }

    @PatchMapping("/{entryId}/paid")
    public EntryResponse setPaid(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @RequestParam(defaultValue = "true") boolean paid) {

        EntryResponse updated = entryService.setPaid(user.userId(), entryId, paid);
        notificationService.synchronize(user.userId());
        return updated;
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @RequestParam(defaultValue = "CURRENT") SeriesScope scope) {

        entryService.delete(user.userId(), entryId, scope);
        notificationService.synchronize(user.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entryId}/subexpenses")
    public ResponseEntity<SubexpenseResponse> addSubexpense(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @Valid @RequestBody SubexpenseRequest request) {

        SubexpenseResponse created = entryService.addSubexpense(user.userId(), entryId, request);
        notificationService.synchronize(user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{entryId}/subexpenses/{subexpenseId}")
    public SubexpenseResponse updateSubexpense(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @PathVariable UUID subexpenseId,
            @RequestParam(defaultValue = "CURRENT") SeriesScope scope,
            @Valid @RequestBody SubexpenseRequest request) {

        SubexpenseResponse updated = entryService.updateSubexpense(user.userId(), entryId, subexpenseId, request, scope);
        notificationService.synchronize(user.userId());
        return updated;
    }

    @DeleteMapping("/{entryId}/subexpenses/{subexpenseId}")
    public ResponseEntity<Void> deleteSubexpense(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @PathVariable UUID subexpenseId,
            @RequestParam(defaultValue = "CURRENT") SeriesScope scope) {

        entryService.deleteSubexpense(user.userId(), entryId, subexpenseId, scope);
        notificationService.synchronize(user.userId());
        return ResponseEntity.noContent().build();
    }
}
