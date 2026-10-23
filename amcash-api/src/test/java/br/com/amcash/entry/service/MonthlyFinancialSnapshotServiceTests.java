package br.com.amcash.entry.service;

import br.com.amcash.entry.entity.EntryCategory;
import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.RecurrenceFrequency;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import br.com.amcash.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyFinancialSnapshotServiceTests {
    private FinancialEntryRepository entryRepository;
    private SubexpenseRepository subexpenseRepository;
    private MonthlyFinancialSnapshotService service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        entryRepository = mock(FinancialEntryRepository.class);
        subexpenseRepository = mock(SubexpenseRepository.class);
        service = new MonthlyFinancialSnapshotService(entryRepository, subexpenseRepository);
        userId = UUID.randomUUID();
        user = new User("subject", "user@amcash.test", "User", null);
        user.setId(userId);
    }

    @Test
    void paidExpenseStopsContributingAndReturnsWhenUnpaid() {
        FinancialEntry expense = expense("Conta", "300", false);
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(expense));

        assertEquals(money("300"), service.load(userId, YearMonth.of(2026, 10)).pendingExpenses());
        expense.setPaid(true);
        assertEquals(0, money("0").compareTo(service.load(userId, YearMonth.of(2026, 10)).pendingExpenses()));
        expense.setPaid(false);
        assertEquals(money("300"), service.load(userId, YearMonth.of(2026, 10)).pendingExpenses());
    }

    @Test
    void detailedExpenseUsesItemsWithoutDuplicatingParentAmount() {
        FinancialEntry invoice = expense("Fatura", "300", true);
        invoice.setPaid(false);
        Subexpense paidItem = new Subexpense(invoice, "Pago", money("100"), "1/2", true);
        Subexpense pendingItem = new Subexpense(invoice, "Pendente", money("150"), "1/3", false);
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(any(), any(), any()))
                .thenReturn(List.of(invoice));
        when(subexpenseRepository.findAllByEntryIdInOrderByEntryIdAscCreatedAtAsc(any()))
                .thenReturn(List.of(paidItem, pendingItem));

        var snapshot = service.load(userId, YearMonth.of(2026, 10));

        assertEquals(money("300"), snapshot.totalExpenses());
        assertEquals(money("100"), snapshot.paidExpenses());
        assertEquals(money("200"), snapshot.pendingExpenses());
    }

    @Test
    void countsOnlyOccurrencesReturnedForTheReferenceMonth() {
        FinancialEntry octoberInstallment = expense("Parcela", "120", false);
        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(octoberInstallment));

        var snapshot = service.load(userId, YearMonth.of(2026, 10));

        assertEquals(money("120"), snapshot.totalExpenses());
        assertEquals(money("120"), snapshot.pendingExpenses());
    }

    private FinancialEntry expense(String name, String amount, boolean detailed) {
        FinancialEntry entry = new FinancialEntry(user, name, EntryCategory.OTHER_EXPENSE, EntryType.EXPENSE,
                money(amount), LocalDate.of(2026, 10, 10), RecurrenceFrequency.MONTHLY,
                12, 0, UUID.randomUUID(), detailed);
        entry.setId(UUID.randomUUID());
        return entry;
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }
}
