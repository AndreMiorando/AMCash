package br.com.amcash.goal.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.goal.entity.GoalMonthPreference;
import br.com.amcash.goal.repository.GoalMonthPreferenceRepository;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GoalService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final FinancialEntryRepository entryRepository;
    private final SubexpenseRepository subexpenseRepository;
    private final UserRepository userRepository;
    private final GoalMonthPreferenceRepository goalPreferenceRepository;

    public GoalService(
            FinancialEntryRepository entryRepository,
            SubexpenseRepository subexpenseRepository,
            UserRepository userRepository,
            GoalMonthPreferenceRepository goalPreferenceRepository) {
        this.entryRepository = entryRepository;
        this.subexpenseRepository = subexpenseRepository;
        this.userRepository = userRepository;
        this.goalPreferenceRepository = goalPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public GoalResponse current(UUID userId, LocalDate referenceDate) {
        return current(userId, referenceDate, YearMonth.from(referenceDate));
    }

    @Transactional(readOnly = true)
    public GoalResponse current(UUID userId, LocalDate referenceDate, YearMonth targetMonth) {
        User user = findUser(userId);
        Set<Integer> selectedDays = goalPreferenceRepository
                .findByUserIdAndYearAndMonth(userId, targetMonth.getYear(), targetMonth.getMonthValue())
                .map(preference -> daysFromMask(preference.getSelectedDaysMask(), targetMonth.lengthOfMonth()))
                .orElseGet(Set::of);
        return calculate(user, referenceDate, targetMonth, selectedDays);
    }

    @Transactional
    public GoalResponse updatePreferences(
            UUID userId,
            LocalDate referenceDate,
            UpdateGoalPreferencesRequest request) {
        return updatePreferences(userId, referenceDate, YearMonth.from(referenceDate), request);
    }

    @Transactional
    public GoalResponse updatePreferences(
            UUID userId,
            LocalDate referenceDate,
            YearMonth targetMonth,
            UpdateGoalPreferencesRequest request) {
        if (request.selectedDays() == null) {
            throw new BadRequestException("Informe os dias disponíveis");
        }
        if (request.selectedDays().stream().anyMatch(day -> day == null || day < 1 || day > targetMonth.lengthOfMonth())) {
            throw new BadRequestException("Existe um dia inválido para o mês selecionado");
        }
        User user = findUser(userId);
        GoalMonthPreference preference = goalPreferenceRepository
                .findByUserIdAndYearAndMonth(userId, targetMonth.getYear(), targetMonth.getMonthValue())
                .orElseGet(() -> new GoalMonthPreference(
                        user,
                        targetMonth.getYear(),
                        targetMonth.getMonthValue(),
                        GoalMonthPreference.ALL_WEEKDAYS_MASK));
        preference.updateSelectedDaysMask(daysToMask(request.selectedDays()));
        goalPreferenceRepository.save(preference);
        return calculate(user, referenceDate, targetMonth, request.selectedDays());
    }

    private GoalResponse calculate(
            User user,
            LocalDate referenceDate,
            YearMonth selectedMonth,
            Set<Integer> selectedDays) {
        List<FinancialEntry> entries = entryRepository
                .findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
                        user.getId(),
                        selectedMonth.atDay(1),
                        selectedMonth.atEndOfMonth());

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

        BigDecimal income = entries.stream()
                .filter(entry -> entry.getType() == EntryType.INCOME)
                .map(FinancialEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingExpenses = entries.stream()
                .filter(entry -> entry.getType() == EntryType.EXPENSE)
                .map(entry -> pendingAmount(
                        entry,
                        itemsByEntryId.getOrDefault(entry.getId(), List.of())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = pendingExpenses.subtract(income).max(BigDecimal.ZERO);
        int availableDays = countRemainingSelectedDays(referenceDate, selectedMonth, selectedDays);
        BigDecimal dailyTarget = remainingAmount.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : availableDays == 0
                        ? remainingAmount.setScale(2, RoundingMode.HALF_UP)
                        : remainingAmount.divide(BigDecimal.valueOf(availableDays), 2, RoundingMode.HALF_UP);
        BigDecimal weeklyTarget = dailyTarget
                .multiply(BigDecimal.valueOf(Math.max(1, Math.min(availableDays, 7))))
                .min(remainingAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal progressPercentage = pendingExpenses.signum() == 0
                ? ONE_HUNDRED
                : income.multiply(ONE_HUNDRED)
                        .divide(pendingExpenses, 2, RoundingMode.HALF_UP)
                        .min(ONE_HUNDRED);

        return new GoalResponse(
                selectedMonth.getYear(),
                selectedMonth.getMonthValue(),
                referenceDate,
                income,
                pendingExpenses,
                remainingAmount,
                weeklyTarget,
                dailyTarget,
                availableDays,
                progressPercentage,
                remainingAmount.signum() == 0,
                selectedDays.stream().sorted().toList());
    }

    private BigDecimal pendingAmount(FinancialEntry entry, List<Subexpense> items) {
        if (entry.isPaid()) return BigDecimal.ZERO;
        if (items.isEmpty()) return entry.getAmount();

        BigDecimal itemTotal = items.stream()
                .map(Subexpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingItems = items.stream()
                .filter(item -> !item.isPaid())
                .map(Subexpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unallocated = entry.getAmount().subtract(itemTotal).max(BigDecimal.ZERO);
        return pendingItems.add(unallocated);
    }

    private int countRemainingSelectedDays(LocalDate referenceDate, YearMonth month, Set<Integer> selectedDays) {
        return (int) selectedDays.stream()
                .map(month::atDay)
                .filter(date -> !date.isBefore(referenceDate))
                .count();
    }

    private int daysToMask(Set<Integer> days) {
        return days.stream()
                .mapToInt(day -> 1 << (day - 1))
                .reduce(0, (mask, day) -> mask | day);
    }

    private Set<Integer> daysFromMask(int mask, int daysInMonth) {
        Set<Integer> days = new HashSet<>();
        for (int day = 1; day <= daysInMonth; day++) {
            if ((mask & (1 << (day - 1))) != 0) days.add(day);
        }
        return days;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
