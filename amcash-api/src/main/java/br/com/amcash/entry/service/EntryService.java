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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        List<SubexpenseRequest> subexpenses = validateCreationSubexpenses(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        boolean detailedExpense = request.type() == EntryType.EXPENSE && request.hasSubexpenses();
        List<LocalDate> occurrenceDates;
        if (detailedExpense) {
            occurrenceDates = subexpenses.stream()
                    .flatMap(item -> java.util.stream.IntStream.range(0, itemOccurrenceCount(item))
                            .mapToObj(index -> recurrenceDate(request.dueDate(), item.recurrenceFrequency(), index)))
                    .distinct()
                    .sorted()
                    .toList();
        } else {
            int totalOccurrences = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                    ? 1
                    : request.recurrenceCount();
            occurrenceDates = java.util.stream.IntStream.range(0, totalOccurrences)
                    .mapToObj(index -> recurrenceDate(request.dueDate(), request.recurrenceFrequency(), index))
                    .toList();
        }

        UUID seriesId = occurrenceDates.size() > 1 ? UUID.randomUUID() : null;
        List<FinancialEntry> entries = new ArrayList<>(occurrenceDates.size());

        for (int index = 0; index < occurrenceDates.size(); index++) {
            LocalDate occurrenceDate = occurrenceDates.get(index);
            BigDecimal occurrenceAmount = detailedExpense
                    ? subexpenses.stream()
                            .filter(item -> itemOccursOn(item, request.dueDate(), occurrenceDate))
                            .map(SubexpenseRequest::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : request.amount();
            entries.add(new FinancialEntry(
                    user,
                    detailedExpense
                            ? request.name().trim()
                            : occurrenceDates.size() > 1
                                    ? request.name().trim() + " - " + (index + 1) + "/" + occurrenceDates.size()
                                    : request.name().trim(),
                    request.category(),
                    request.type(),
                    occurrenceAmount,
                    occurrenceDate,
                    request.recurrenceFrequency(),
                    request.recurrenceCount(),
                    index,
                    seriesId,
                    detailedExpense
            ));
        }

        List<FinancialEntry> savedEntries = entryRepository.saveAll(entries);
        if (detailedExpense) {
            List<Subexpense> savedSubexpenses = new ArrayList<>();
            for (SubexpenseRequest item : subexpenses) {
                int itemOccurrences = itemOccurrenceCount(item);
                UUID itemSeriesId = itemOccurrences > 1 ? UUID.randomUUID() : null;
                for (int itemIndex = 0; itemIndex < itemOccurrences; itemIndex++) {
                    LocalDate itemDate = recurrenceDate(request.dueDate(), item.recurrenceFrequency(), itemIndex);
                    FinancialEntry parent = savedEntries.stream()
                            .filter(entry -> entry.getDueDate().equals(itemDate))
                            .findFirst()
                            .orElseThrow();
                    savedSubexpenses.add(new Subexpense(
                            parent,
                            item.name().trim(),
                            item.amount(),
                            itemOccurrences > 1 ? (itemIndex + 1) + "/" + itemOccurrences : null,
                            false,
                            item.recurrenceFrequency(),
                            item.recurrenceCount(),
                            itemIndex,
                            itemSeriesId));
                }
            }
            subexpenseRepository.saveAll(savedSubexpenses);
        }

        return new CreatedEntriesResponse(toResponses(savedEntries));
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
                toResponses(entries)
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
        boolean hasStoredSubexpenses = subexpenseRepository.existsByEntryId(entryId);

        if (hasStoredSubexpenses && (request.type() == EntryType.INCOME || !request.hasSubexpenses())) {
            throw new BadRequestException("Remova os itens antes de desativar essa opção ou transformar em receita");
        }

        if (request.type() == EntryType.EXPENSE && request.hasSubexpenses()) {
            return updateDetailedEntry(userId, selectedEntry, request);
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

    private EntryResponse updateDetailedEntry(
            UUID userId,
            FinancialEntry selectedEntry,
            UpdateEntryRequest request) {

        List<FinancialEntry> series = selectedEntry.getSeriesId() == null
                ? List.of(selectedEntry)
                : entryRepository.findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(
                        selectedEntry.getSeriesId(), userId);
        String name = baseName(request.name());

        for (FinancialEntry entry : series) {
            boolean selected = entry.getId().equals(selectedEntry.getId());
            entry.update(
                    name,
                    request.category(),
                    EntryType.EXPENSE,
                    selected ? sumSubexpenses(entry.getId()) : entry.getAmount(),
                    selected ? request.dueDate() : entry.getDueDate(),
                    RecurrenceFrequency.NONE,
                    0,
                    entry.getRecurrenceIndex(),
                    entry.getSeriesId(),
                    true);
        }

        entryRepository.saveAll(series);
        return toResponse(selectedEntry);
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
        validateRecurrence(request.recurrenceFrequency(), request.recurrenceCount());
        FinancialEntry entry = findOwnedEntry(userId, entryId);
        if (entry.getType() != EntryType.EXPENSE || !entry.isHasSubexpenses()) {
            throw new BadRequestException("Este lançamento não aceita itens");
        }

        if (request.recurrenceFrequency() == RecurrenceFrequency.NONE) {
            BigDecimal currentTotal = sumSubexpenses(entryId);
            Subexpense subexpense = new Subexpense(
                    entry,
                    request.name().trim(),
                    request.amount(),
                    normalizeNullable(request.installmentDescription()),
                    request.paid(),
                    RecurrenceFrequency.NONE,
                    0,
                    0,
                    null);
            Subexpense saved = subexpenseRepository.save(subexpense);
            updateEntryAmount(entry, currentTotal.add(saved.getAmount()));
            return toResponse(saved);
        }

        List<LocalDate> occurrenceDates = new ArrayList<>(request.recurrenceCount());
        for (int index = 0; index < request.recurrenceCount(); index++) {
            occurrenceDates.add(recurrenceDate(entry.getDueDate(), request.recurrenceFrequency(), index));
        }
        List<FinancialEntry> parents = ensureDetailedParentEntries(entry, userId, occurrenceDates, request.amount());
        Map<UUID, BigDecimal> previousTotals = sumSubexpensesByEntryIds(
                parents.stream().map(FinancialEntry::getId).toList());
        UUID itemSeriesId = UUID.randomUUID();
        List<Subexpense> occurrences = new ArrayList<>(request.recurrenceCount());

        for (int index = 0; index < request.recurrenceCount(); index++) {
            occurrences.add(new Subexpense(
                    parents.get(index),
                    request.name().trim(),
                    request.amount(),
                    (index + 1) + "/" + request.recurrenceCount(),
                    index == 0 && request.paid(),
                    request.recurrenceFrequency(),
                    request.recurrenceCount(),
                    index,
                    itemSeriesId));
        }

        subexpenseRepository.saveAll(occurrences);
        for (FinancialEntry parent : parents) {
            parent.updateAmount(previousTotals.getOrDefault(parent.getId(), BigDecimal.ZERO).add(request.amount()));
        }
        entryRepository.saveAll(parents);
        return toResponse(occurrences.get(0));
    }

    @Transactional
    public SubexpenseResponse updateSubexpense(
            UUID userId,
            UUID entryId,
            UUID subexpenseId,
            SubexpenseRequest request) {

        validateRecurrence(request.recurrenceFrequency(), request.recurrenceCount());
        Subexpense selected = findOwnedSubexpense(userId, entryId, subexpenseId);
        List<Subexpense> originalSeries = selected.getSeriesId() == null
                ? new ArrayList<>(List.of(selected))
                : new ArrayList<>(subexpenseRepository
                        .findAllBySeriesIdAndEntryUserIdOrderByRecurrenceIndexAsc(
                                selected.getSeriesId(), userId));
        if (originalSeries.stream().noneMatch(item -> item.getId().equals(selected.getId()))) {
            originalSeries.add(selected);
        }

        int selectedIndex = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                ? 0
                : selected.getRecurrenceIndex();
        if (request.recurrenceFrequency() != RecurrenceFrequency.NONE
                && selectedIndex >= request.recurrenceCount()) {
            throw new BadRequestException("O total de repetições não pode ser menor que a repetição atual");
        }

        List<FinancialEntry> parents;
        UUID itemSeriesId;
        if (request.recurrenceFrequency() == RecurrenceFrequency.NONE) {
            parents = List.of(selected.getEntry());
            itemSeriesId = null;
        } else {
            LocalDate initialDate = recurrenceDate(
                    selected.getEntry().getDueDate(),
                    request.recurrenceFrequency(),
                    -selectedIndex);
            List<LocalDate> occurrenceDates = new ArrayList<>(request.recurrenceCount());
            for (int index = 0; index < request.recurrenceCount(); index++) {
                occurrenceDates.add(recurrenceDate(initialDate, request.recurrenceFrequency(), index));
            }
            parents = ensureDetailedParentEntries(selected.getEntry(), userId, occurrenceDates, request.amount());
            itemSeriesId = selected.getSeriesId() == null ? UUID.randomUUID() : selected.getSeriesId();
        }

        Map<UUID, FinancialEntry> affectedParents = new LinkedHashMap<>();
        originalSeries.forEach(item -> affectedParents.put(item.getEntry().getId(), item.getEntry()));
        parents.forEach(parent -> affectedParents.put(parent.getId(), parent));
        Map<UUID, BigDecimal> storedTotals = sumSubexpensesByEntryIds(affectedParents.keySet());
        Map<FinancialEntry, BigDecimal> totals = new LinkedHashMap<>();
        affectedParents.forEach((affectedEntryId, parent) ->
                totals.put(parent, storedTotals.getOrDefault(affectedEntryId, BigDecimal.ZERO)));
        for (Subexpense item : originalSeries) {
            totals.computeIfPresent(item.getEntry(), (parent, total) -> total.subtract(item.getAmount()));
        }
        for (FinancialEntry parent : parents) {
            totals.computeIfPresent(parent, (item, total) -> total.add(request.amount()));
        }

        int totalOccurrences = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                ? 1
                : request.recurrenceCount();
        List<Subexpense> synchronizedSeries = new ArrayList<>(totalOccurrences);
        for (int index = 0; index < totalOccurrences; index++) {
            int occurrenceIndex = index;
            Subexpense occurrence = originalSeries.stream()
                    .filter(item -> item.getRecurrenceIndex() == occurrenceIndex)
                    .findFirst()
                    .orElseGet(() -> new Subexpense(
                            parents.get(occurrenceIndex),
                            request.name().trim(),
                            request.amount(),
                            null,
                            false));
            occurrence.synchronize(
                    parents.get(index),
                    request.name().trim(),
                    request.amount(),
                    totalOccurrences > 1 ? (index + 1) + "/" + totalOccurrences : null,
                    index == selectedIndex ? request.paid() : occurrence.isPaid(),
                    request.recurrenceFrequency(),
                    request.recurrenceFrequency() == RecurrenceFrequency.NONE ? 0 : totalOccurrences,
                    index,
                    itemSeriesId);
            synchronizedSeries.add(occurrence);
        }

        List<Subexpense> removed = originalSeries.stream()
                .filter(item -> !synchronizedSeries.contains(item))
                .toList();
        if (!removed.isEmpty()) subexpenseRepository.deleteAll(removed);
        subexpenseRepository.saveAll(synchronizedSeries);
        totals.forEach(FinancialEntry::updateAmount);
        entryRepository.saveAll(new ArrayList<>(totals.keySet()));
        return toResponse(synchronizedSeries.get(selectedIndex));
    }

    @Transactional
    public void deleteSubexpense(UUID userId, UUID entryId, UUID subexpenseId) {
        Subexpense subexpense = findOwnedSubexpense(userId, entryId, subexpenseId);
        BigDecimal currentTotal = sumSubexpenses(entryId);
        subexpenseRepository.delete(subexpense);
        updateEntryAmount(subexpense.getEntry(), currentTotal.subtract(subexpense.getAmount()));
    }

    private List<FinancialEntry> ensureDetailedParentEntries(
            FinancialEntry selectedEntry,
            UUID userId,
            List<LocalDate> occurrenceDates,
            BigDecimal initialAmount) {

        List<FinancialEntry> series = selectedEntry.getSeriesId() == null
                ? new ArrayList<>(List.of(selectedEntry))
                : new ArrayList<>(entryRepository.findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(
                        selectedEntry.getSeriesId(), userId));
        if (series.stream().noneMatch(entry -> entry.getId().equals(selectedEntry.getId()))) {
            series.add(selectedEntry);
        }
        UUID seriesId = selectedEntry.getSeriesId() == null ? UUID.randomUUID() : selectedEntry.getSeriesId();
        String name = baseName(selectedEntry.getName());

        for (LocalDate occurrenceDate : occurrenceDates) {
            boolean exists = series.stream().anyMatch(entry -> entry.getDueDate().equals(occurrenceDate));
            if (!exists) {
                series.add(new FinancialEntry(
                        selectedEntry.getUser(),
                        name,
                        selectedEntry.getCategory(),
                        EntryType.EXPENSE,
                        initialAmount,
                        occurrenceDate,
                        selectedEntry.getRecurrenceFrequency(),
                        selectedEntry.getRecurrenceCount(),
                        0,
                        seriesId,
                        true));
            }
        }

        series.sort(Comparator.comparing(FinancialEntry::getDueDate));
        for (int index = 0; index < series.size(); index++) {
            FinancialEntry parent = series.get(index);
            parent.update(
                    name,
                    parent.getCategory(),
                    EntryType.EXPENSE,
                    parent.getAmount(),
                    parent.getDueDate(),
                    parent.getRecurrenceFrequency(),
                    parent.getRecurrenceCount(),
                    index,
                    seriesId,
                    true);
        }
        entryRepository.saveAll(series);

        return occurrenceDates.stream()
                .map(date -> series.stream()
                        .filter(entry -> entry.getDueDate().equals(date))
                        .findFirst()
                        .orElseThrow())
                .toList();
    }
    private BigDecimal sumSubexpenses(UUID entryId) {
        return sumSubexpensesByEntryIds(List.of(entryId)).getOrDefault(entryId, BigDecimal.ZERO);
    }

    private Map<UUID, BigDecimal> sumSubexpensesByEntryIds(Iterable<UUID> entryIds) {
        List<UUID> ids = new ArrayList<>();
        entryIds.forEach(ids::add);
        if (ids.isEmpty()) return Map.of();

        Map<UUID, BigDecimal> totals = new HashMap<>();
        subexpenseRepository.sumAmountsByEntryIds(ids)
                .forEach(total -> totals.put(total.getEntryId(), total.getTotal()));
        return totals;
    }

    private void updateEntryAmount(FinancialEntry entry, BigDecimal amount) {
        entry.updateAmount(amount.max(BigDecimal.ZERO));
        entryRepository.save(entry);
    }

    private FinancialEntry findOwnedEntry(UUID userId, UUID entryId) {
        return entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));
    }

    private Subexpense findOwnedSubexpense(UUID userId, UUID entryId, UUID subexpenseId) {
        return subexpenseRepository.findByIdAndEntryIdAndEntryUserId(subexpenseId, entryId, userId)
                .orElseThrow(() -> new NotFoundException("Subdespesa não encontrada"));
    }

    private List<EntryResponse> toResponses(List<FinancialEntry> entries) {
        if (entries.isEmpty()) return List.of();

        List<UUID> entryIds = entries.stream().map(FinancialEntry::getId).toList();
        Map<UUID, List<SubexpenseResponse>> subexpensesByEntryId = new HashMap<>();
        for (Subexpense subexpense : subexpenseRepository
                .findAllByEntryIdInOrderByEntryIdAscCreatedAtAsc(entryIds)) {
            subexpensesByEntryId
                    .computeIfAbsent(subexpense.getEntry().getId(), ignored -> new ArrayList<>())
                    .add(toResponse(subexpense));
        }

        Set<UUID> seriesIds = new LinkedHashSet<>();
        entries.stream()
                .map(FinancialEntry::getSeriesId)
                .filter(java.util.Objects::nonNull)
                .forEach(seriesIds::add);
        Map<UUID, SeriesSummary> summariesBySeriesId = new HashMap<>();
        if (!seriesIds.isEmpty()) {
            UUID userId = entries.get(0).getUser().getId();
            entryRepository.summarizeSeries(userId, seriesIds).forEach(summary ->
                    summariesBySeriesId.put(
                            summary.getSeriesId(),
                            new SeriesSummary(
                                    Math.toIntExact(summary.getTotalOccurrences()),
                                    Math.toIntExact(summary.getPaidOccurrences()))));
        }

        return entries.stream()
                .map(entry -> {
                    List<SubexpenseResponse> subexpenses = subexpensesByEntryId
                            .getOrDefault(entry.getId(), List.of());
                    SeriesSummary summary = entry.getSeriesId() == null
                            ? new SeriesSummary(1, entry.isPaid() ? 1 : 0)
                            : summariesBySeriesId.getOrDefault(
                                    entry.getSeriesId(),
                                    new SeriesSummary(1, entry.isPaid() ? 1 : 0));
                    return toResponse(entry, subexpenses, summary);
                })
                .toList();
    }

    private EntryResponse toResponse(FinancialEntry entry) {
        List<SubexpenseResponse> subexpenses = subexpenseRepository
                .findAllByEntryIdOrderByCreatedAtAsc(entry.getId())
                .stream()
                .map(this::toResponse)
                .toList();
        SeriesSummary summary;
        if (entry.getSeriesId() == null) {
            summary = new SeriesSummary(1, entry.isPaid() ? 1 : 0);
        } else {
            summary = entryRepository
                    .summarizeSeries(entry.getUser().getId(), List.of(entry.getSeriesId()))
                    .stream()
                    .findFirst()
                    .map(statistics -> new SeriesSummary(
                            Math.toIntExact(statistics.getTotalOccurrences()),
                            Math.toIntExact(statistics.getPaidOccurrences())))
                    .orElse(new SeriesSummary(1, entry.isPaid() ? 1 : 0));
        }
        return toResponse(entry, subexpenses, summary);
    }

    private EntryResponse toResponse(
            FinancialEntry entry,
            List<SubexpenseResponse> subexpenses,
            SeriesSummary summary) {

        int completed = (int) subexpenses.stream().filter(SubexpenseResponse::paid).count();
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
                summary.paidOccurrences(),
                summary.totalOccurrences(),
                completed,
                subexpenses.size(),
                subexpenses,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private record SeriesSummary(int totalOccurrences, int paidOccurrences) {
    }
    private SubexpenseResponse toResponse(Subexpense subexpense) {
        return new SubexpenseResponse(
                subexpense.getId(),
                subexpense.getName(),
                subexpense.getAmount(),
                subexpense.getInstallmentDescription(),
                subexpense.isPaid(),
                subexpense.getRecurrenceFrequency(),
                subexpense.getRecurrenceCount(),
                subexpense.getRecurrenceIndex(),
                subexpense.getSeriesId(),
                subexpense.getCreatedAt(),
                subexpense.getUpdatedAt()
        );
    }

    private List<SubexpenseRequest> validateCreationSubexpenses(CreateEntryRequest request) {
        List<SubexpenseRequest> subexpenses = request.subexpenses() == null ? List.of() : request.subexpenses();
        if (request.hasSubexpenses() && subexpenses.isEmpty()) {
            throw new BadRequestException("Uma despesa detalhada precisa ter pelo menos um item");
        }
        if (!request.hasSubexpenses() && !subexpenses.isEmpty()) {
            throw new BadRequestException("Itens só podem ser informados em uma despesa detalhada");
        }
        subexpenses.forEach(item -> validateRecurrence(item.recurrenceFrequency(), item.recurrenceCount()));
        if (!subexpenses.isEmpty()) {
            BigDecimal itemsTotal = subexpenses.stream()
                    .map(SubexpenseRequest::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (itemsTotal.compareTo(request.amount()) != 0) {
                throw new BadRequestException("O valor da despesa deve corresponder à soma dos itens");
            }
        }
        return subexpenses;
    }

    private int itemOccurrenceCount(SubexpenseRequest item) {
        return item.recurrenceFrequency() == RecurrenceFrequency.NONE ? 1 : item.recurrenceCount();
    }

    private boolean itemOccursOn(SubexpenseRequest item, LocalDate initialDate, LocalDate occurrenceDate) {
        for (int index = 0; index < itemOccurrenceCount(item); index++) {
            if (recurrenceDate(initialDate, item.recurrenceFrequency(), index).equals(occurrenceDate)) {
                return true;
            }
        }
        return false;
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
            throw new BadRequestException("Receitas não podem possuir itens");
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
