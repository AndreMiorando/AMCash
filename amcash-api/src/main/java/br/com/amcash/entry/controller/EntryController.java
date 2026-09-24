package br.com.amcash.entry.controller;

import br.com.amcash.config.AuthenticatedUser;
import br.com.amcash.config.CurrentUser;
import br.com.amcash.entry.dto.request.CreateEntryRequest;
import br.com.amcash.entry.dto.request.SubexpenseRequest;
import br.com.amcash.entry.dto.request.UpdateEntryRequest;
import br.com.amcash.entry.dto.response.CreatedEntriesResponse;
import br.com.amcash.entry.dto.response.EntryResponse;
import br.com.amcash.entry.dto.response.MonthlyEntriesResponse;
import br.com.amcash.entry.dto.response.SubexpenseResponse;
import br.com.amcash.entry.service.EntryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping
    public MonthlyEntriesResponse listMonth(
            @CurrentUser AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam int month) {

        return entryService.listMonth(user.userId(), year, month);
    }

    @PostMapping
    public ResponseEntity<CreatedEntriesResponse> create(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateEntryRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entryService.create(user.userId(), request));
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
            @Valid @RequestBody UpdateEntryRequest request) {

        return entryService.update(user.userId(), entryId, request);
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId) {

        entryService.delete(user.userId(), entryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{entryId}/subexpenses")
    public ResponseEntity<SubexpenseResponse> addSubexpense(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @Valid @RequestBody SubexpenseRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entryService.addSubexpense(user.userId(), entryId, request));
    }

    @PutMapping("/{entryId}/subexpenses/{subexpenseId}")
    public SubexpenseResponse updateSubexpense(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @PathVariable UUID subexpenseId,
            @Valid @RequestBody SubexpenseRequest request) {

        return entryService.updateSubexpense(user.userId(), entryId, subexpenseId, request);
    }

    @DeleteMapping("/{entryId}/subexpenses/{subexpenseId}")
    public ResponseEntity<Void> deleteSubexpense(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID entryId,
            @PathVariable UUID subexpenseId) {

        entryService.deleteSubexpense(user.userId(), entryId, subexpenseId);
        return ResponseEntity.noContent().build();
    }
}
