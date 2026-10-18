package br.com.amcash.entry.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MonthlyFinancialSnapshotService {

    private final FinancialEntryRepository entryRepository;
    private final SubexpenseRepository subexpenseRepository;

    public MonthlyFinancialSnapshotService(
            FinancialEntryRepository entryRepository,
            SubexpenseRepository subexpenseRepository) {
        this.entryRepository = entryRepository;
        this.subexpenseRepository = subexpenseRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyFinancialSnapshot load(UUID userId, YearMonth month) {
        List<FinancialEntry> entries = entryRepository
                .findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                        userId, month.atDay(1), month.atEndOfMonth());
        List<UUID> detailedEntryIds = entries.stream()
                .filter(FinancialEntry::isHasSubexpenses)
                .map(FinancialEntry::getId)
                .toList();
        Map<UUID, List<Subexpense>> itemsByEntryId = new HashMap<>();
        if (!detailedEntryIds.isEmpty()) {
            for (Subexpense item : subexpenseRepository
                    .findAllByEntryIdInOrderByEntryIdAscCreatedAtAsc(detailedEntryIds)) {
                itemsByEntryId.computeIfAbsent(item.getEntry().getId(), ignored -> new ArrayList<>()).add(item);
            }
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal paidExpenses = BigDecimal.ZERO;
        BigDecimal pendingExpenses = BigDecimal.ZERO;
        for (FinancialEntry entry : entries) {
            if (entry.getType() == EntryType.INCOME) {
                income = income.add(entry.getAmount());
                continue;
            }
            totalExpenses = totalExpenses.add(entry.getAmount());
            PaymentSplit split = paymentSplit(entry, itemsByEntryId.getOrDefault(entry.getId(), List.of()));
            paidExpenses = paidExpenses.add(split.paid());
            pendingExpenses = pendingExpenses.add(split.pending());
        }
        return new MonthlyFinancialSnapshot(income, totalExpenses, paidExpenses, pendingExpenses);
    }

    private PaymentSplit paymentSplit(FinancialEntry entry, List<Subexpense> items) {
        if (entry.isPaid()) return new PaymentSplit(entry.getAmount(), BigDecimal.ZERO);
        if (items.isEmpty()) return new PaymentSplit(BigDecimal.ZERO, entry.getAmount());

        BigDecimal paidItems = items.stream().filter(Subexpense::isPaid)
                .map(Subexpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingItems = items.stream().filter(item -> !item.isPaid())
                .map(Subexpense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unallocated = entry.getAmount().subtract(paidItems.add(pendingItems)).max(BigDecimal.ZERO);
        return new PaymentSplit(paidItems, pendingItems.add(unallocated));
    }

    private record PaymentSplit(BigDecimal paid, BigDecimal pending) {
    }

    public record MonthlyFinancialSnapshot(
            BigDecimal income,
            BigDecimal totalExpenses,
            BigDecimal paidExpenses,
            BigDecimal pendingExpenses) {
    }
}
