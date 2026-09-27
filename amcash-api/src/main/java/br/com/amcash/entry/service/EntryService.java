package br.com.amcash.entry.service;

import br.com.amcash.entry.dto.request.CreateEntryRequest;
import br.com.amcash.entry.dto.request.SubexpenseRequest;
import br.com.amcash.entry.dto.request.UpdateEntryRequest;
import br.com.amcash.entry.dto.response.CreatedEntriesResponse;
import br.com.amcash.entry.dto.response.EntryResponse;
import br.com.amcash.entry.dto.response.ForecastEventResponse;
import br.com.amcash.entry.dto.response.ForecastMonthResponse;
import br.com.amcash.entry.dto.response.ForecastResponse;
import br.com.amcash.entry.dto.response.MonthlyEntriesResponse;
import br.com.amcash.entry.dto.response.MonthlySummaryResponse;
import br.com.amcash.entry.dto.response.SubexpenseResponse;
import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import br.com.amcash.entry.entity.SeriesScope;
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
                .filter(entry -> entry.getType() == EntryType.EXPENSE && !entry.isPaid())
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
    public ForecastResponse forecast(UUID userId, int year, int month, int months) {
        if (months != 6 && months != 12) {
            throw new BadRequestException("O horizonte do Radar deve ser de 6 ou 12 meses");
        }

        YearMonth startMonth;
        try {
            startMonth = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new BadRequestException("Ano ou mês inválido");
        }

        YearMonth queryStart = startMonth.minusMonths(1);
        YearMonth endMonth = startMonth.plusMonths(months - 1L);
        List<FinancialEntry> entries = entryRepository
                .findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                        userId,
                        queryStart.atDay(1),
                        endMonth.atEndOfMonth());

        List<UUID> detailedEntryIds = entries.stream()
                .filter(FinancialEntry::isHasSubexpenses)
                .map(FinancialEntry::getId)
                .toList();
        Map<UUID, List<Subexpense>> itemsByEntryId = new HashMap<>();
        if (!detailedEntryIds.isEmpty()) {
            for (Subexpense item : subexpenseRepository
                    .findAllByEntryIdInOrderByEntryIdAscCreatedAtAsc(detailedEntryIds)) {
                itemsByEntryId
                        .computeIfAbsent(item.getEntry().getId(), ignored -> new ArrayList<>())
                        .add(item);
            }
        }

        Map<YearMonth, ForecastTotals> totalsByMonth = new LinkedHashMap<>();
        for (int offset = -1; offset < months; offset++) {
            totalsByMonth.put(startMonth.plusMonths(offset), new ForecastTotals());
        }

        for (FinancialEntry entry : entries) {
            YearMonth entryMonth = YearMonth.from(entry.getDueDate());
            ForecastTotals totals = totalsByMonth.get(entryMonth);
            if (totals == null) continue;

            if (entry.getType() == EntryType.INCOME) {
                totals.income = totals.income.add(entry.getAmount());
            } else {
                totals.totalExpenses = totals.totalExpenses.add(entry.getAmount());
                addExpensePaymentTotals(
                        totals,
                        entry,
                        itemsByEntryId.getOrDefault(entry.getId(), List.of()));
            }

            if (!entry.isHasSubexpenses()) {
                addEntryForecastEvents(totals, entry);
            }
            itemsByEntryId.getOrDefault(entry.getId(), List.of())
                    .forEach(item -> addItemForecastEvents(totals, item));
        }

        List<ForecastMonthResponse> timeline = new ArrayList<>(months);
        ForecastTotals previous = totalsByMonth.get(queryStart);
        for (int offset = 0; offset < months; offset++) {
            YearMonth period = startMonth.plusMonths(offset);
            ForecastTotals current = totalsByMonth.get(period);
            current.events.sort(Comparator
                    .comparing(ForecastEventResponse::date)
                    .thenComparing(ForecastEventResponse::type)
                    .thenComparing(ForecastEventResponse::name));

            BigDecimal projectedBalance = current.income.subtract(current.totalExpenses);
            BigDecimal freeBalance = current.income.subtract(current.pendingExpenses);
            BigDecimal previousFreeBalance = previous.income.subtract(previous.pendingExpenses);
            timeline.add(new ForecastMonthResponse(
                    period.getYear(),
                    period.getMonthValue(),
                    current.income,
                    current.totalExpenses,
                    current.paidExpenses,
                    current.pendingExpenses,
                    projectedBalance,
                    freeBalance,
                    current.totalExpenses.subtract(previous.totalExpenses),
                    freeBalance.subtract(previousFreeBalance),
                    List.copyOf(current.events)));
            previous = current;
        }

        return new ForecastResponse(year, month, months, timeline);
    }

    private void addExpensePaymentTotals(
            ForecastTotals totals,
            FinancialEntry entry,
            List<Subexpense> items) {
        if (entry.isPaid()) {
            totals.paidExpenses = totals.paidExpenses.add(entry.getAmount());
            return;
        }
        if (items.isEmpty()) {
            totals.pendingExpenses = totals.pendingExpenses.add(entry.getAmount());
            return;
        }

        BigDecimal paidItems = items.stream()
                .filter(Subexpense::isPaid)
                .map(Subexpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingItems = items.stream()
                .filter(item -> !item.isPaid())
                .map(Subexpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unallocated = entry.getAmount().subtract(paidItems.add(pendingItems));
        totals.paidExpenses = totals.paidExpenses.add(paidItems);
        totals.pendingExpenses = totals.pendingExpenses.add(pendingItems.add(unallocated.max(BigDecimal.ZERO)));
    }

    private void addEntryForecastEvents(ForecastTotals totals, FinancialEntry entry) {
        if (entry.getSeriesId() == null || entry.getRecurrenceCount() < 2) return;
        String name = baseName(entry.getName());
        if (entry.getRecurrenceIndex() == 0) {
            totals.events.add(new ForecastEventResponse(
                    "STARTING", "ENTRY", name, entry.getAmount(), entry.getDueDate(),
                    entry.getRecurrenceFrequency().name()));
        }
        if (entry.getRecurrenceIndex() == entry.getRecurrenceCount() - 1) {
            totals.events.add(new ForecastEventResponse(
                    "ENDING", "ENTRY", name, entry.getAmount(), entry.getDueDate(),
                    entry.getRecurrenceFrequency().name()));
        }
    }

    private void addItemForecastEvents(ForecastTotals totals, Subexpense item) {
        if (item.getSeriesId() == null || item.getRecurrenceCount() < 2) return;
        if (item.getRecurrenceIndex() == 0) {
            totals.events.add(new ForecastEventResponse(
                    "STARTING", "ITEM", item.getName(), item.getAmount(), item.getEntry().getDueDate(),
                    item.getRecurrenceFrequency().name()));
        }
        if (item.getRecurrenceIndex() == item.getRecurrenceCount() - 1) {
            totals.events.add(new ForecastEventResponse(
                    "ENDING", "ITEM", item.getName(), item.getAmount(), item.getEntry().getDueDate(),
                    item.getRecurrenceFrequency().name()));
        }
    }

    private static final class ForecastTotals {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal totalExpenses = BigDecimal.ZERO;
        private BigDecimal paidExpenses = BigDecimal.ZERO;
        private BigDecimal pendingExpenses = BigDecimal.ZERO;
        private final List<ForecastEventResponse> events = new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public EntryResponse get(UUID userId, UUID entryId) {
        return toResponse(findOwnedEntry(userId, entryId));
    }

    @Transactional
    public EntryResponse update(UUID userId, UUID entryId, UpdateEntryRequest request, SeriesScope scope) {
        validateRecurrence(request.recurrenceFrequency(), request.recurrenceCount());
        validateSubexpenses(request.type(), request.hasSubexpenses());
        validateCategory(request.type(), request.category());
        FinancialEntry selectedEntry = findOwnedEntry(userId, entryId);
        boolean hasStoredSubexpenses = subexpenseRepository.existsByEntryId(entryId);

        if (hasStoredSubexpenses && (request.type() == EntryType.INCOME || !request.hasSubexpenses())) {
            throw new BadRequestException("Remova os itens antes de desativar essa opção ou transformar em receita");
        }

        if (request.type() == EntryType.EXPENSE && request.hasSubexpenses()) {
            return updateDetailedEntry(userId, selectedEntry, request, scope);
        }

        String baseName = baseName(request.name());
        if (scope == SeriesScope.CURRENT && selectedEntry.getSeriesId() != null) {
            selectedEntry.update(
                    request.name().trim(),
                    request.category(),
                    request.type(),
                    request.amount(),
                    request.dueDate(),
                    selectedEntry.getRecurrenceFrequency(),
                    selectedEntry.getRecurrenceCount(),
                    selectedEntry.getRecurrenceIndex(),
                    selectedEntry.getSeriesId(),
                    false);
            return toResponse(entryRepository.save(selectedEntry));
        }

        int totalOccurrences = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                ? 1
                : request.recurrenceCount();

        if (totalOccurrences == 1) {
            if (selectedEntry.getSeriesId() != null) {
                List<FinancialEntry> series = entryRepository
                        .findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(selectedEntry.getSeriesId(), userId);
                entryRepository.deleteAll(series.stream()
                        .filter(item -> scope == SeriesScope.ALL
                                ? !item.getId().equals(selectedEntry.getId())
                                : item.getRecurrenceIndex() > selectedEntry.getRecurrenceIndex())
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

        List<FinancialEntry> removed = series.stream()
                .filter(item -> item.getRecurrenceIndex() >= totalOccurrences)
                .toList();
        if (!removed.isEmpty()) entryRepository.deleteAll(removed);
        series.removeAll(removed);

        int startIndex = selectedEntry.getSeriesId() == null || scope == SeriesScope.ALL
                ? 0
                : selectedEntry.getRecurrenceIndex();
        List<FinancialEntry> changed = new ArrayList<>();
        for (int index = startIndex; index < totalOccurrences; index++) {
            String installmentName = baseName + " - " + (index + 1) + "/" + totalOccurrences;
            LocalDate installmentDate = recurrenceDate(initialDate, request.recurrenceFrequency(), index);
            int occurrenceIndex = index;
            FinancialEntry occurrence = series.stream()
                    .filter(item -> item.getRecurrenceIndex() == occurrenceIndex)
                    .findFirst()
                    .orElse(null);

            if (occurrence != null) {
                occurrence.update(
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
                changed.add(occurrence);
            } else {
                FinancialEntry created = new FinancialEntry(
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
                        request.type() == EntryType.EXPENSE && request.hasSubexpenses());
                series.add(created);
                changed.add(created);
            }
        }

        entryRepository.saveAll(changed);
        return toResponse(selectedEntry);
    }

    private EntryResponse updateDetailedEntry(
            UUID userId,
            FinancialEntry selectedEntry,
            UpdateEntryRequest request,
            SeriesScope scope) {

        List<FinancialEntry> series = selectedEntry.getSeriesId() == null
                ? List.of(selectedEntry)
                : entryRepository.findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(
                        selectedEntry.getSeriesId(), userId);
        String name = baseName(request.name());

        List<FinancialEntry> affected = series.stream()
                .filter(entry -> scope == SeriesScope.CURRENT
                        ? entry.getId().equals(selectedEntry.getId())
                        : scope == SeriesScope.ALL
                                || entry.getRecurrenceIndex() >= selectedEntry.getRecurrenceIndex())
                .toList();
        for (FinancialEntry entry : affected) {
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

        entryRepository.saveAll(affected);
        return toResponse(selectedEntry);
    }

    @Transactional
    public EntryResponse setPaid(UUID userId, UUID entryId, boolean paid) {
        FinancialEntry entry = findOwnedEntry(userId, entryId);
        entry.setPaid(paid);
        return toResponse(entryRepository.save(entry));
    }

    @Transactional
    public void delete(UUID userId, UUID entryId, SeriesScope scope) {
        FinancialEntry selected = findOwnedEntry(userId, entryId);
        if (scope == SeriesScope.CURRENT || selected.getSeriesId() == null) {
            entryRepository.delete(selected);
            return;
        }
        List<FinancialEntry> future = entryRepository
                .findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(selected.getSeriesId(), userId)
                .stream()
                .filter(entry -> scope == SeriesScope.ALL
                        || entry.getRecurrenceIndex() >= selected.getRecurrenceIndex())
                .toList();
        entryRepository.deleteAll(future);
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
            SubexpenseRequest request,
            SeriesScope scope) {

        validateRecurrence(request.recurrenceFrequency(), request.recurrenceCount());
        Subexpense selected = findOwnedSubexpense(userId, entryId, subexpenseId);
        if (scope == SeriesScope.CURRENT && selected.getSeriesId() != null) {
            BigDecimal currentTotal = sumSubexpenses(selected.getEntry().getId());
            BigDecimal previousAmount = selected.getAmount();
            selected.synchronize(
                    selected.getEntry(),
                    request.name().trim(),
                    request.amount(),
                    selected.getInstallmentDescription(),
                    request.paid(),
                    selected.getRecurrenceFrequency(),
                    selected.getRecurrenceCount(),
                    selected.getRecurrenceIndex(),
                    selected.getSeriesId());
            Subexpense saved = subexpenseRepository.save(selected);
            updateEntryAmount(
                    selected.getEntry(),
                    currentTotal.subtract(previousAmount).add(request.amount()));
            return toResponse(saved);
        }

        List<Subexpense> originalSeries = selected.getSeriesId() == null
                ? new ArrayList<>(List.of(selected))
                : new ArrayList<>(subexpenseRepository
                        .findAllBySeriesIdAndEntryUserIdOrderByRecurrenceIndexAsc(
                                selected.getSeriesId(), userId));
        if (originalSeries.stream().noneMatch(item -> item.getId().equals(selected.getId()))) {
            originalSeries.add(selected);
        }

        int selectedIndex = selected.getSeriesId() == null ? 0 : selected.getRecurrenceIndex();
        if (request.recurrenceFrequency() != RecurrenceFrequency.NONE
                && selectedIndex >= request.recurrenceCount()) {
            throw new BadRequestException("O total de repetições não pode ser menor que a repetição atual");
        }

        List<Subexpense> affectedOriginalSeries = scope == SeriesScope.CURRENT_AND_FUTURE
                ? originalSeries.stream()
                        .filter(item -> item.getRecurrenceIndex() >= selectedIndex)
                        .toList()
                : originalSeries;

        List<FinancialEntry> parents;
        List<FinancialEntry> targetParents;
        UUID itemSeriesId;
        if (request.recurrenceFrequency() == RecurrenceFrequency.NONE) {
            parents = List.of(selected.getEntry());
            targetParents = parents;
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
            int firstTargetIndex = scope == SeriesScope.CURRENT_AND_FUTURE ? selectedIndex : 0;
            targetParents = parents.subList(firstTargetIndex, parents.size());
            itemSeriesId = selected.getSeriesId() == null ? UUID.randomUUID() : selected.getSeriesId();
        }

        Map<UUID, FinancialEntry> affectedParents = new LinkedHashMap<>();
        affectedOriginalSeries.forEach(item -> affectedParents.put(item.getEntry().getId(), item.getEntry()));
        targetParents.forEach(parent -> affectedParents.put(parent.getId(), parent));
        Map<UUID, BigDecimal> storedTotals = sumSubexpensesByEntryIds(affectedParents.keySet());
        Map<FinancialEntry, BigDecimal> totals = new LinkedHashMap<>();
        affectedParents.forEach((affectedEntryId, parent) ->
                totals.put(parent, storedTotals.getOrDefault(affectedEntryId, BigDecimal.ZERO)));
        for (Subexpense item : affectedOriginalSeries) {
            totals.computeIfPresent(item.getEntry(), (parent, total) -> total.subtract(item.getAmount()));
        }
        for (FinancialEntry parent : targetParents) {
            totals.computeIfPresent(parent, (item, total) -> total.add(request.amount()));
        }

        int totalOccurrences = request.recurrenceFrequency() == RecurrenceFrequency.NONE
                ? 1
                : request.recurrenceCount();
        int firstSynchronizedIndex = scope == SeriesScope.CURRENT_AND_FUTURE
                && request.recurrenceFrequency() != RecurrenceFrequency.NONE
                ? selectedIndex
                : 0;
        List<Subexpense> synchronizedSeries = new ArrayList<>(totalOccurrences - firstSynchronizedIndex);
        for (int index = firstSynchronizedIndex; index < totalOccurrences; index++) {
            int occurrenceIndex = index;
            Subexpense occurrence = affectedOriginalSeries.stream()
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

        List<Subexpense> removed = affectedOriginalSeries.stream()
                .filter(item -> !synchronizedSeries.contains(item))
                .toList();
        if (!removed.isEmpty()) subexpenseRepository.deleteAll(removed);
        subexpenseRepository.saveAll(synchronizedSeries);
        totals.forEach(FinancialEntry::updateAmount);
        entryRepository.saveAll(new ArrayList<>(totals.keySet()));
        return toResponse(synchronizedSeries.get(0));
    }

    @Transactional
    public void deleteSubexpense(
            UUID userId,
            UUID entryId,
            UUID subexpenseId,
            SeriesScope scope) {
        Subexpense selected = findOwnedSubexpense(userId, entryId, subexpenseId);
        List<Subexpense> targets;
        if (scope == SeriesScope.CURRENT || selected.getSeriesId() == null) {
            targets = List.of(selected);
        } else {
            List<Subexpense> series = subexpenseRepository
                    .findAllBySeriesIdAndEntryUserIdOrderByRecurrenceIndexAsc(selected.getSeriesId(), userId);
            targets = scope == SeriesScope.ALL
                    ? series
                    : series.stream()
                            .filter(item -> item.getRecurrenceIndex() >= selected.getRecurrenceIndex())
                            .toList();
        }

        Map<UUID, FinancialEntry> affectedParents = new LinkedHashMap<>();
        targets.forEach(item -> affectedParents.put(item.getEntry().getId(), item.getEntry()));
        Map<UUID, BigDecimal> storedTotals = sumSubexpensesByEntryIds(affectedParents.keySet());
        Map<FinancialEntry, BigDecimal> totals = new LinkedHashMap<>();
        affectedParents.forEach((affectedEntryId, parent) ->
                totals.put(parent, storedTotals.getOrDefault(affectedEntryId, BigDecimal.ZERO)));
        targets.forEach(item -> totals.computeIfPresent(
                item.getEntry(),
                (parent, total) -> total.subtract(item.getAmount())));

        subexpenseRepository.deleteAll(targets);
        totals.forEach(FinancialEntry::updateAmount);
        entryRepository.saveAll(new ArrayList<>(totals.keySet()));
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
