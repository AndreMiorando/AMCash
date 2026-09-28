package br.com.amcash.goal.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.entity.Subexpense;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.entry.repository.SubexpenseRepository;
import br.com.amcash.goal.dto.request.UpdateGoalPreferencesRequest;
import br.com.amcash.goal.dto.response.GoalResponse;
import br.com.amcash.shared.exception.BadRequestException;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.user.entity.User;
import br.com.amcash.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
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

    public GoalService(
            FinancialEntryRepository entryRepository,
            SubexpenseRepository subexpenseRepository,
            UserRepository userRepository) {
        this.entryRepository = entryRepository;
        this.subexpenseRepository = subexpenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GoalResponse current(UUID userId, LocalDate referenceDate) {
        return current(userId, referenceDate, YearMonth.from(referenceDate));
    }

    @Transactional(readOnly = true)
    public GoalResponse current(UUID userId, LocalDate referenceDate, YearMonth targetMonth) {
        return calculate(findUser(userId), referenceDate, targetMonth);
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
        if (request.availableWeekdays() == null || request.availableWeekdays().isEmpty()) {
            throw new BadRequestException("Selecione ao menos um dia disponível");
        }
        User user = findUser(userId);
        user.updateGoalWeekdaysMask(toMask(request.availableWeekdays()));
        userRepository.save(user);
        return calculate(user, referenceDate, targetMonth);
    }

    private GoalResponse calculate(User user, LocalDate referenceDate, YearMonth selectedMonth) {
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
        Set<DayOfWeek> availableWeekdays = fromMask(user.getGoalWeekdaysMask());
        LocalDate firstAvailableDate = referenceDate.plusDays(1).isAfter(selectedMonth.atDay(1))
                ? referenceDate.plusDays(1)
                : selectedMonth.atDay(1);
        int availableDays = countAvailableDays(firstAvailableDate, selectedMonth.atEndOfMonth(), availableWeekdays);
        BigDecimal dailyTarget = remainingAmount.signum() == 0 || availableDays == 0
                ? BigDecimal.ZERO.setScale(2)
                : remainingAmount.divide(BigDecimal.valueOf(availableDays), 2, RoundingMode.HALF_UP);
        BigDecimal weeklyTarget = dailyTarget
                .multiply(BigDecimal.valueOf(Math.min(availableWeekdays.size(), Math.max(availableDays, 1))))
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
                availableWeekdays.stream().sorted().toList());
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

    private int countAvailableDays(LocalDate start, LocalDate end, Set<DayOfWeek> availableWeekdays) {
        int count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (availableWeekdays.contains(date.getDayOfWeek())) count++;
        }
        return count;
    }

    private int toMask(Set<DayOfWeek> weekdays) {
        return weekdays.stream()
                .mapToInt(day -> 1 << (day.getValue() - 1))
                .reduce(0, (mask, day) -> mask | day);
    }

    private Set<DayOfWeek> fromMask(int mask) {
        EnumSet<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if ((mask & (1 << (day.getValue() - 1))) != 0) weekdays.add(day);
        }
        return weekdays.isEmpty()
                ? EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                : weekdays;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
