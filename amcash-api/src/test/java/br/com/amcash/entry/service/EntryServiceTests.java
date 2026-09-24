package br.com.amcash.entry.service;

import br.com.amcash.entry.dto.request.CreateEntryRequest;
import br.com.amcash.entry.dto.request.SubexpenseRequest;
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
        CreateEntryRequest request = new CreateEntryRequest(
                "Nubank", EntryCategory.FINANCIAL, EntryType.EXPENSE, new BigDecimal("332.00"),
                LocalDate.of(2026, 1, 31), RecurrenceFrequency.MONTHLY, 2, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entryRepository.saveAll(any())).thenAnswer(invocation -> {
            List<FinancialEntry> entries = invocation.getArgument(0);
            entries.forEach(entry -> entry.setId(UUID.randomUUID()));
            return entries;
        });
        when(subexpenseRepository.findAllByEntryIdOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());

        CreatedEntriesResponse response = entryService.create(userId, request);

        assertEquals(3, response.entries().size());
        assertEquals("Nubank - 1/3", response.entries().get(0).name());
        assertEquals(LocalDate.of(2026, 1, 31), response.entries().get(0).dueDate());
        assertEquals(LocalDate.of(2026, 2, 28), response.entries().get(1).dueDate());
        assertEquals(LocalDate.of(2026, 3, 31), response.entries().get(2).dueDate());
        assertNotNull(response.entries().get(0).seriesId());
        assertEquals(response.entries().get(0).seriesId(), response.entries().get(2).seriesId());
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
    void shouldRejectInvalidRecurrenceAndProtectOtherUsersData() {
        UUID userId = UUID.randomUUID();
        CreateEntryRequest request = new CreateEntryRequest(
                "Salário", EntryCategory.SALARY, EntryType.INCOME, new BigDecimal("8500.00"),
                LocalDate.of(2026, 10, 31), RecurrenceFrequency.NONE, 1, false);

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
                false);

        assertThrows(
                BadRequestException.class,
                () -> entryService.create(UUID.randomUUID(), request));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldAddSubexpenseOnlyToEnabledExpense() {
        UUID userId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        FinancialEntry parent = entry(
                user(userId), EntryType.EXPENSE, "500.00", LocalDate.of(2026, 10, 10), true);
        parent.setId(entryId);
        when(entryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(parent));
        when(subexpenseRepository.save(any(Subexpense.class))).thenAnswer(invocation -> {
            Subexpense subexpense = invocation.getArgument(0);
            subexpense.setId(UUID.randomUUID());
            return subexpense;
        });

        var response = entryService.addSubexpense(
                userId,
                entryId,
                new SubexpenseRequest("Mercado", new BigDecimal("120.00"), "1/2", true));

        assertEquals("Mercado", response.name());
        assertEquals(new BigDecimal("120.00"), response.amount());
        assertTrue(response.paid());

        FinancialEntry income = entry(
                user(userId), EntryType.INCOME, "8500.00", LocalDate.of(2026, 10, 31), false);
        income.setId(entryId);
        when(entryRepository.findByIdAndUserId(entryId, userId)).thenReturn(Optional.of(income));

        assertThrows(
                BadRequestException.class,
                () -> entryService.addSubexpense(
                        userId,
                        entryId,
                        new SubexpenseRequest("Item", BigDecimal.TEN, null, false)));
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
