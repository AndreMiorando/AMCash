package br.com.amcash.entry.service;

import br.com.amcash.entry.dto.request.CreateEntryRequest;
import br.com.amcash.entry.dto.request.SubexpenseRequest;
import br.com.amcash.entry.dto.request.UpdateEntryRequest;
import br.com.amcash.entry.dto.response.CreatedEntriesResponse;
import br.com.amcash.entry.dto.response.MonthlyEntriesResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntryServiceTests {

    private FinancialEntryRepository entryRepository;
    private SubexpenseRepository subexpenseRepository;
    private UserRepository userRepository;
    private EntryService entryService;

    @BeforeEach
    void setUp() {
        entryRepository = mock(FinancialEntryRepository.class);
        subexpenseRepository = mock(SubexpenseRepository.class);
        userRepository = mock(UserRepository.class);
        entryService = new EntryService(entryRepository, subexpenseRepository, userRepository);
    }

    @Test
    void shouldCreateOriginalAndMonthlyRecurrences() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        List<Subexpense> savedItems = new ArrayList<>();
        CreateEntryRequest request = new CreateEntryRequest(
                "Nubank", EntryCategory.FINANCIAL, EntryType.EXPENSE, new BigDecimal("432.00"),
                LocalDate.of(2026, 1, 31), RecurrenceFrequency.MONTHLY, 2, true,
                List.of(
                        new SubexpenseRequest("Fatura", new BigDecimal("332.00"), null, false, RecurrenceFrequency.MONTHLY, 2),
                        new SubexpenseRequest("Mercado", new BigDecimal("100.00"), null, false, RecurrenceFrequency.NONE, 0)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entryRepository.saveAll(any())).thenAnswer(invocation -> {
            List<FinancialEntry> entries = invocation.getArgument(0);
            entries.forEach(entry -> entry.setId(UUID.randomUUID()));
            return entries;
        });
        when(subexpenseRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Subexpense> items = invocation.getArgument(0);
            savedItems.addAll(items);
            return items;
        });
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());

        CreatedEntriesResponse response = entryService.create(userId, request);

        assertEquals(2, response.entries().size());
        assertEquals("Nubank", response.entries().get(0).name());
        assertEquals(LocalDate.of(2026, 1, 31), response.entries().get(0).dueDate());
        assertEquals(LocalDate.of(2026, 2, 28), response.entries().get(1).dueDate());
        assertEquals(new BigDecimal("432.00"), response.entries().get(0).amount());
        assertEquals(new BigDecimal("332.00"), response.entries().get(1).amount());
        assertNotNull(response.entries().get(0).seriesId());
        assertEquals(response.entries().get(0).seriesId(), response.entries().get(1).seriesId());
        assertEquals(3, savedItems.size());
        assertEquals("1/2", savedItems.get(0).getInstallmentDescription());
        assertEquals("2/2", savedItems.get(1).getInstallmentDescription());
        assertEquals(null, savedItems.get(2).getInstallmentDescription());
        verify(subexpenseRepository).saveAll(any());
    }

    @Test
    void shouldResizeSeriesAndRefreshInstallmentDescriptions() {
        UUID userId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        User user = user(userId);
        FinancialEntry first = new FinancialEntry(
                user, "Internet - 1/3", EntryCategory.BILLS_AND_SERVICES, EntryType.EXPENSE,
                new BigDecimal("100.00"), LocalDate.of(2026, 9, 24),
                RecurrenceFrequency.MONTHLY, 3, 0, seriesId, false);
        FinancialEntry second = new FinancialEntry(
                user, "Internet - 2/3", EntryCategory.BILLS_AND_SERVICES, EntryType.EXPENSE,
                new BigDecimal("100.00"), LocalDate.of(2026, 10, 24),
                RecurrenceFrequency.MONTHLY, 3, 1, seriesId, false);
        FinancialEntry third = new FinancialEntry(
                user, "Internet - 3/3", EntryCategory.BILLS_AND_SERVICES, EntryType.EXPENSE,
                new BigDecimal("100.00"), LocalDate.of(2026, 11, 24),
                RecurrenceFrequency.MONTHLY, 3, 2, seriesId, false);
        first.setId(UUID.randomUUID());
        second.setId(UUID.randomUUID());
        third.setId(UUID.randomUUID());
        List<FinancialEntry> series = List.of(first, second, third);

        when(entryRepository.findByIdAndUserId(first.getId(), userId)).thenReturn(Optional.of(first));
        when(entryRepository.findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(seriesId, userId))
                .thenReturn(series);
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        var response = entryService.update(
                userId,
                first.getId(),
                new UpdateEntryRequest(
                        "Internet - 1/3",
                        EntryCategory.BILLS_AND_SERVICES,
                        EntryType.EXPENSE,
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 9, 24),
                        RecurrenceFrequency.MONTHLY,
                        2,
                        false));

        assertEquals("Internet - 1/2", response.name());
        assertEquals(2, response.recurrenceCount());
        assertEquals("Internet - 2/2", second.getName());
        verify(entryRepository).deleteAll(List.of(third));
        verify(entryRepository).saveAll(List.of(first, second));
    }

    @Test
    void shouldCalculateMonthlySummary() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        List<FinancialEntry> entries = List.of(
                entry(user, EntryType.INCOME, "8500.00", LocalDate.of(2026, 10, 31), false),
                entry(user, EntryType.EXPENSE, "1097.00", LocalDate.of(2026, 10, 1), false),
                entry(user, EntryType.EXPENSE, "277.90", LocalDate.of(2026, 10, 8), false));

        when(entryRepository.findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(entries);
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());

        MonthlyEntriesResponse response = entryService.listMonth(userId, 2026, 10);

        assertEquals(new BigDecimal("8500.00"), response.summary().income());
        assertEquals(new BigDecimal("1374.90"), response.summary().expenses());
        assertEquals(new BigDecimal("7125.10"), response.summary().balance());
        assertEquals(3, response.entries().size());
    }

    @Test
    void shouldMarkEntryAsPaid() {
        UUID userId = UUID.randomUUID();
        FinancialEntry entry = entry(
                user(userId), EntryType.EXPENSE, "250.00", LocalDate.of(2026, 9, 24), false);

        when(entryRepository.findByIdAndUserId(entry.getId(), userId)).thenReturn(Optional.of(entry));
        when(entryRepository.save(entry)).thenReturn(entry);
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(entry.getId()))
                .thenReturn(List.of());

        var response = entryService.setPaid(userId, entry.getId(), true);

        assertTrue(entry.isPaid());
        assertTrue(response.paid());
        assertEquals(1, response.paidOccurrences());
        assertEquals(1, response.totalOccurrences());
    }

    @Test
    void shouldRejectInvalidRecurrenceAndProtectOtherUsersData() {
        UUID userId = UUID.randomUUID();
        CreateEntryRequest request = new CreateEntryRequest(
                "Salário", EntryCategory.SALARY, EntryType.INCOME, new BigDecimal("8500.00"),
                LocalDate.of(2026, 10, 31), RecurrenceFrequency.NONE, 1, false, List.of());

        assertThrows(BadRequestException.class, () -> entryService.create(userId, request));
        verify(userRepository, never()).findById(any());

        UUID entryId = UUID.randomUUID();
        when(entryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> entryService.get(userId, entryId));
    }

    @Test
    void shouldRejectCategoryFromAnotherEntryType() {
        CreateEntryRequest request = new CreateEntryRequest(
                "Salário",
                EntryCategory.FOOD,
                EntryType.INCOME,
                new BigDecimal("8500.00"),
                LocalDate.of(2026, 10, 31),
                RecurrenceFrequency.NONE,
                0,
                false,
                List.of());

        assertThrows(
                BadRequestException.class,
                () -> entryService.create(UUID.randomUUID(), request));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldRejectDetailedExpenseWithoutValidItemsOrMatchingTotal() {
        UUID userId = UUID.randomUUID();
        CreateEntryRequest withoutItems = new CreateEntryRequest(
                "Fatura", EntryCategory.FINANCIAL, EntryType.EXPENSE, new BigDecimal("100.00"),
                LocalDate.of(2026, 10, 10), RecurrenceFrequency.NONE, 0, true, List.of());
        CreateEntryRequest mismatchedTotal = new CreateEntryRequest(
                "Fatura", EntryCategory.FINANCIAL, EntryType.EXPENSE, new BigDecimal("100.00"),
                LocalDate.of(2026, 10, 10), RecurrenceFrequency.NONE, 0, true,
                List.of(new SubexpenseRequest("Streaming", new BigDecimal("59.90"), null, false, RecurrenceFrequency.NONE, 0)));

        assertThrows(BadRequestException.class, () -> entryService.create(userId, withoutItems));
        assertThrows(BadRequestException.class, () -> entryService.create(userId, mismatchedTotal));
        verify(userRepository, never()).findById(any());
    }
    @Test
    void shouldAddSubexpenseOnlyToEnabledExpense() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        FinancialEntry parent = entry(
                user(userId), EntryType.EXPENSE, "34.00", LocalDate.of(2026, 10, 10), true);
        parent.setId(entryId);
        Subexpense existing = new Subexpense(parent, "Existente", new BigDecimal("34.00"), null, false);
        existing.setId(UUID.randomUUID());
        when(entryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(parent));
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(entryId)).thenReturn(List.of(existing));
        when(subexpenseRepository.save(any(Subexpense.class))).thenAnswer(invocation -> {
            Subexpense subexpense = invocation.getArgument(0);
            subexpense.setId(UUID.randomUUID());
            return subexpense;
        });

        var response = entryService.addSubexpense(
                userId,
                entryId,
                new SubexpenseRequest("Mercado", new BigDecimal("120.00"), "1/2", true, RecurrenceFrequency.NONE, 0));

        assertEquals("Mercado", response.name());
        assertEquals(new BigDecimal("120.00"), response.amount());
        assertTrue(response.paid());
        assertEquals(new BigDecimal("154.00"), parent.getAmount());
        verify(entryRepository).save(parent);

        FinancialEntry income = entry(
                user(userId), EntryType.INCOME, "8500.00", LocalDate.of(2026, 10, 31), false);
        income.setId(entryId);
        when(entryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(income));

        assertThrows(
                BadRequestException.class,
                () -> entryService.addSubexpense(
                        userId,
                        entryId,
                        new SubexpenseRequest("Item", BigDecimal.TEN, null, false, RecurrenceFrequency.NONE, 0)));
    }

    @Test
    void shouldRecalculateDetailedExpenseWhenUpdatingAndDeletingItem() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        FinancialEntry parent = entry(
                user(userId), EntryType.EXPENSE, "142.00", LocalDate.of(2026, 10, 10), true);
        parent.setId(entryId);
        Subexpense edited = new Subexpense(parent, "Chat", new BigDecimal("108.00"), "1/12", false);
        edited.setId(itemId);
        Subexpense other = new Subexpense(parent, "Mercado", new BigDecimal("34.00"), null, false);
        other.setId(UUID.randomUUID());

        when(subexpenseRepository.findByIdAndEntryIdAndEntryUserId(itemId, entryId, userId))
                .thenReturn(Optional.of(edited));
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(entryId))
                .thenReturn(List.of(edited, other));
        when(subexpenseRepository.save(edited)).thenReturn(edited);

        entryService.updateSubexpense(
                userId,
                entryId,
                itemId,
                new SubexpenseRequest("Chat", new BigDecimal("120.00"), "1/12", false, RecurrenceFrequency.NONE, 0));

        assertEquals(new BigDecimal("154.00"), parent.getAmount());

        entryService.deleteSubexpense(userId, entryId, itemId);

        assertEquals(new BigDecimal("34.00"), parent.getAmount());
        verify(subexpenseRepository).delete(edited);
    }
    private User user(UUID id) {
        User user = new User("google-subject", "usuario@gmail.com", "Usuário", null);
        user.setId(id);
        return user;
    }

    private FinancialEntry entry(
            User user,
            EntryType type,
            String amount,
            LocalDate dueDate,
            boolean hasSubexpenses) {

        FinancialEntry entry = new FinancialEntry(
                user,
                type == EntryType.INCOME ? "Receita" : "Despesa",
                type == EntryType.INCOME ? EntryCategory.OTHER_INCOME : EntryCategory.OTHER_EXPENSE,
                type,
                new BigDecimal(amount),
                dueDate,
                RecurrenceFrequency.NONE,
                0,
                0,
                null,
                hasSubexpenses);
        entry.setId(UUID.randomUUID());
        return entry;
    }
}
