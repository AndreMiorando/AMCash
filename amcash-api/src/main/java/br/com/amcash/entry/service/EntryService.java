package br.com.amcash.entry.service;

import br.com.amcash.entry.dto.request.CreateEntryRequest;
import br.com.amcash.entry.dto.request.SubexpenseRequest;
import br.com.amcash.entry.dto.request.UpdateEntryRequest;
import br.com.amcash.entry.dto.response.CreatedEntriesResponse;
import br.com.amcash.entry.dto.response.EntryResponse;
import br.com.amcash.entry.dto.response.MonthlyEntriesResponse;
import br.com.amcash.entry.dto.response.MonthlySummaryResponse;
import br.com.amcash.entry.dto.response.SubexpenseResponse;
import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EntryService {

    private final FinancialEntryRepository entryRepository;
    private final SubexpenseRepository subexpenseRepository;
    private final UserRepository userRepository;

    public EntryService(
            FinancialEntryRepository entryRepository,
            SubexpenseRepository subexpenseRepository,
            UserRepository userRepository) {

        this.entryRepository = entryRepository;
        this.subexpenseRepository = subexpenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CreatedEntriesResponse create(UUID userId, CreateEntryRequest request) {
        validateRecurrence(request.recurrenceFrequency(), request.recurrenceCount());
        validateSubexpenses(request.type(), request.hasSubexpenses());
        validateCategory(request.type(), request.category());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        int totalOccurrences = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                ? 1
                : request.recurrenceCount();
        UUID seriesId = totalOccurrences > 1 ? UUID.randomUUID() : null;
        List<FinancialEntry> entries = new ArrayList<>(totalOccurrences);

        for (int index = 0; index < totalOccurrences; index++) {
            entries.add(new FinancialEntry(
                    user,
                    totalOccurrences > 1
                            ? request.name().trim() + " - " + (index + 1) + "/" + totalOccurrences
                            : request.name().trim(),
                    request.category(),
                    request.type(),
                    request.amount(),
                    recurrenceDate(request.dueDate(), request.recurrenceFrequency(), index),
                    request.recurrenceFrequency(),
                    request.recurrenceCount(),
                    index,
                    seriesId,
                    request.type() == EntryType.EXPENSE && request.hasSubexpenses()
            ));
        }

        return new CreatedEntriesResponse(entryRepository.saveAll(entries).stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public MonthlyEntriesResponse listMonth(UUID userId, int year, int month) {
        YearMonth selectedMonth;
        try {
            selectedMonth = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BadRequestException("Ano ou mês inválido");
        }

        List<FinancialEntry> entries = entryRepository
                .findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                        userId,
                        selectedMonth.atDay(1),
                        selectedMonth.atEndOfMonth());

        BigDecimal income = entries.stream()
                .filter(entry -> entry.getType() == EntryType.INCOME)
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = entries.stream()
                .filter(entry -> entry.getType() == EntryType.EXPENSE)
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MonthlyEntriesResponse(
                year,
                month,
                new MonthlySummaryResponse(income, expenses, income.subtract(expenses)),
                entries.stream().map(this::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public EntryResponse get(UUID userId, UUID entryId) {
        return toResponse(findOwnedEntry(userId, entryId));
    }

    @Transactional
    public EntryResponse update(UUID userId, UUID entryId, UpdateEntryRequest request) {
        validateRecurrence(request.recurrenceFrequency(), request.recurrenceCount());
        validateSubexpenses(request.type(), request.hasSubexpenses());
        validateCategory(request.type(), request.category());
        FinancialEntry selectedEntry = findOwnedEntry(userId, entryId);
        boolean hasStoredSubexpenses = !subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(entryId).isEmpty();

        if (hasStoredSubexpenses && (request.type() == EntryType.INCOME || !request.hasSubexpenses())) {
            throw new BadRequestException("Remova as subdespesas antes de desativar essa opção ou transformar em receita");
        }

        String baseName = baseName(request.name());
        int totalOccurrences = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                ? 1
                : request.recurrenceCount();

        if (totalOccurrences == 1) {
            if (selectedEntry.getSeriesId() != null) {
                List<FinancialEntry> series = entryRepository
                        .findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(selectedEntry.getSeriesId(), userId);
                entryRepository.deleteAll(series.stream()
                        .filter(item -> !item.getId().equals(selectedEntry.getId()))
                        .toList());
            }

            selectedEntry.update(
                    baseName,
                    request.category(),
                    request.type(),
                    request.amount(),
                    request.dueDate(),
                    RecurrenceFrequency.NONE,
                    0,
                    0,
                    null,
                    request.type() == EntryType.EXPENSE && request.hasSubexpenses());
            return toResponse(entryRepository.save(selectedEntry));
        }

        if (selectedEntry.getRecurrenceIndex() >= totalOccurrences) {
            throw new BadRequestException("O total de parcelas não pode ser menor que a parcela atual");
        }

        List<FinancialEntry> series = selectedEntry.getSeriesId() == null
                ? new ArrayList<>(List.of(selectedEntry))
                : new ArrayList<>(entryRepository.findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(
                        selectedEntry.getSeriesId(), userId));
        UUID seriesId = selectedEntry.getSeriesId() == null ? UUID.randomUUID() : selectedEntry.getSeriesId();
        LocalDate initialDate = recurrenceDate(
                request.dueDate(),
                request.recurrenceFrequency(),
                -selectedEntry.getRecurrenceIndex());

        if (series.size() > totalOccurrences) {
            entryRepository.deleteAll(new ArrayList<>(series.subList(totalOccurrences, series.size())));
            series = new ArrayList<>(series.subList(0, totalOccurrences));
        }

        for (int index = 0; index < totalOccurrences; index++) {
            String installmentName = baseName + " - " + (index + 1) + "/" + totalOccurrences;
            LocalDate installmentDate = recurrenceDate(initialDate, request.recurrenceFrequency(), index);

            if (index < series.size()) {
                series.get(index).update(
                        installmentName,
                        request.category(),
                        request.type(),
                        request.amount(),
                        installmentDate,
                        request.recurrenceFrequency(),
                        totalOccurrences,
                        index,
                        seriesId,
                        request.type() == EntryType.EXPENSE && request.hasSubexpenses());
            } else {
                series.add(new FinancialEntry(
                        selectedEntry.getUser(),
                        installmentName,
                        request.category(),
                        request.type(),
                        request.amount(),
                        installmentDate,
                        request.recurrenceFrequency(),
                        totalOccurrences,
                        index,
                        seriesId,
                        request.type() == EntryType.EXPENSE && request.hasSubexpenses()));
            }
        }

        entryRepository.saveAll(series);
        return toResponse(series.get(selectedEntry.getRecurrenceIndex()));
    }
    @Transactional
    public EntryResponse setPaid(UUID userId, UUID entryId, boolean paid) {
        FinancialEntry entry = findOwnedEntry(userId, entryId);
        entry.setPaid(paid);
        return toResponse(entryRepository.save(entry));
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        entryRepository.delete(findOwnedEntry(userId, entryId));
    }

    @Transactional
    public SubexpenseResponse addSubexpense(UUID userId, UUID entryId, SubexpenseRequest request) {
        FinancialEntry entry = findOwnedEntry(userId, entryId);
        if (entry.getType() != EntryType.EXPENSE || !entry.isHasSubexpenses()) {
            throw new BadRequestException("Este lançamento não aceita subdespesas");
        }

        Subexpense subexpense = new Subexpense(
                entry,
                request.name().trim(),
                request.amount(),
                normalizeNullable(request.installmentDescription()),
                request.paid());
        return toResponse(subexpenseRepository.save(subexpense));
    }

    @Transactional
    public SubexpenseResponse updateSubexpense(
            UUID userId,
            UUID entryId,
            UUID subexpenseId,
            SubexpenseRequest request) {

        Subexpense subexpense = findOwnedSubexpense(userId, entryId, subexpenseId);
        subexpense.update(
                request.name().trim(),
                request.amount(),
                normalizeNullable(request.installmentDescription()),
                request.paid());
        return toResponse(subexpenseRepository.save(subexpense));
    }

    @Transactional
    public void deleteSubexpense(UUID userId, UUID entryId, UUID subexpenseId) {
        subexpenseRepository.delete(findOwnedSubexpense(userId, entryId, subexpenseId));
    }

    private FinancialEntry findOwnedEntry(UUID userId, UUID entryId) {
        return entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));
    }

    private Subexpense findOwnedSubexpense(UUID userId, UUID entryId, UUID subexpenseId) {
        return subexpenseRepository.findByIdAndEntryIdAndEntryUserId(subexpenseId, entryId, userId)
                .orElseThrow(() -> new NotFoundException("Subdespesa não encontrada"));
    }

    private EntryResponse toResponse(FinancialEntry entry) {
        List<SubexpenseResponse> subexpenses = subexpenseRepository
                .findAllByEntryIdOrderByCreatedAtAsc(entry.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        int completed = (int) subexpenses.stream().filter(SubexpenseResponse::paid).count();
        int totalOccurrences = entry.getSeriesId() == null
                ? 1
                : Math.toIntExact(entryRepository.countBySeriesIdAndUserId(
                        entry.getSeriesId(), entry.getUser().getId()));
        int paidOccurrences = entry.getSeriesId() == null
                ? (entry.isPaid() ? 1 : 0)
                : Math.toIntExact(entryRepository.countBySeriesIdAndUserIdAndPaidTrue(
                        entry.getSeriesId(), entry.getUser().getId()));

        return new EntryResponse(
                entry.getId(),
                entry.getName(),
                entry.getCategory(),
                entry.getType(),
                entry.getAmount(),
                entry.getDueDate(),
                entry.getRecurrenceFrequency(),
                entry.getRecurrenceCount(),
                entry.getRecurrenceIndex(),
                entry.getSeriesId(),
                entry.isHasSubexpenses(),
                entry.isPaid(),
                paidOccurrences,
                totalOccurrences,
                completed,
                subexpenses.size(),
                subexpenses,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private SubexpenseResponse toResponse(Subexpense subexpense) {
        return new SubexpenseResponse(
                subexpense.getId(),
                subexpense.getName(),
                subexpense.getAmount(),
                subexpense.getInstallmentDescription(),
                subexpense.isPaid(),
                subexpense.getCreatedAt(),
                subexpense.getUpdatedAt()
        );
    }

    private void validateRecurrence(RecurrenceFrequency frequency, int count) {
        if (frequency == RecurrenceFrequency.NONE && count != 0) {
            throw new BadRequestException("Uma transação sem repetição deve usar recurrenceCount igual a zero");
        }
        if (frequency != RecurrenceFrequency.NONE && count < 2) {
            throw new BadRequestException("Informe ao menos duas parcelas");
        }
    }

    private void validateSubexpenses(EntryType type, boolean hasSubexpenses) {
        if (type == EntryType.INCOME && hasSubexpenses) {
            throw new BadRequestException("Receitas não podem possuir subdespesas");
        }
    }

    private void validateCategory(EntryType type, EntryCategory category) {
        if (!category.supports(type)) {
            throw new BadRequestException("A categoria selecionada não pertence ao tipo do lançamento");
        }
    }

    private String baseName(String name) {
        return name.trim().replaceFirst("\\s+-\\s+\\d+/\\d+$", "").trim();
    }
    private LocalDate recurrenceDate(LocalDate initialDate, RecurrenceFrequency frequency, int index) {
        return switch (frequency) {
            case DAILY -> initialDate.plusDays(index);
            case WEEKLY -> initialDate.plusWeeks(index);
            case MONTHLY -> initialDate.plusMonths(index);
            case NONE -> initialDate;
        };
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
