package br.com.amcash.notification.service;

import br.com.amcash.entry.entity.EntryType;
import br.com.amcash.entry.entity.FinancialEntry;
import br.com.amcash.entry.repository.FinancialEntryRepository;
import br.com.amcash.notification.dto.NotificationResponse;
import br.com.amcash.notification.entity.FinancialAlertType;
import br.com.amcash.notification.entity.FinancialNotification;
import br.com.amcash.notification.repository.FinancialNotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class FinancialNotificationService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final FinancialEntryRepository entryRepository;
    private final FinancialNotificationRepository notificationRepository;

    public FinancialNotificationService(FinancialEntryRepository entryRepository, FinancialNotificationRepository notificationRepository) {
        this.entryRepository = entryRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public List<NotificationResponse> list(UUID userId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        reconcile(userId, entryRepository.findAllByUserIdAndTypeAndPaidFalseAndDueDateLessThanEqualOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, today.plusDays(3)), today);
        return notificationRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .sorted(Comparator.comparingInt((FinancialNotification item) -> group(item, today))
                        .thenComparing(item -> item.getEntry().getDueDate()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void synchronize(UUID userId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        reconcile(userId, entryRepository.findAllByUserIdAndTypeAndPaidFalseAndDueDateLessThanEqualOrderByDueDateAscCreatedAtAsc(
                userId, EntryType.EXPENSE, today.plusDays(3)), today);
    }

    @Transactional
    public List<NotificationResponse> markAllRead(UUID userId) {
        List<NotificationResponse> current = list(userId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        notificationRepository.findAllByUserIdAndActiveTrue(userId).forEach(item -> item.markRead(now));
        return current.stream().map(item -> new NotificationResponse(item.id(), item.type(), item.entryId(), item.expenseName(), item.amount(), item.dueDate(), item.createdAt(), item.readAt() == null ? now : item.readAt())).toList();
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void reconcileDaily() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Map<UUID, List<FinancialEntry>> byUser = new LinkedHashMap<>();
        entryRepository.findAllByTypeAndPaidFalseAndDueDateLessThanEqualOrderByUserIdAscDueDateAsc(EntryType.EXPENSE, today.plusDays(3))
                .forEach(entry -> byUser.computeIfAbsent(entry.getUser().getId(), ignored -> new ArrayList<>()).add(entry));
        notificationRepository.findAllByActiveTrue()
                .forEach(item -> byUser.computeIfAbsent(item.getEntry().getUser().getId(), ignored -> new ArrayList<>()));
        byUser.forEach((userId, entries) -> reconcile(userId, entries, today));
    }

    private void reconcile(UUID userId, List<FinancialEntry> entries, LocalDate today) {
        Map<UUID, FinancialNotification> existing = new HashMap<>();
        notificationRepository.findAllByUserIdAndActiveTrue(userId).forEach(item -> existing.put(item.getEntry().getId(), item));
        for (FinancialEntry entry : entries) {
            FinancialAlertType type = typeFor(entry.getDueDate(), today);
            FinancialNotification notification = existing.remove(entry.getId());
            if (notification == null) notification = notificationRepository.findByUserIdAndEntryId(userId, entry.getId()).orElse(null);
            if (notification == null) notificationRepository.save(new FinancialNotification(entry.getUser(), entry, type));
            else notification.activate(type);
        }
        existing.values().forEach(FinancialNotification::resolve);
    }

    private int group(FinancialNotification item, LocalDate today) {
        return group(item.getAlertType(), item.getEntry().getDueDate(), today);
    }

    static FinancialAlertType typeFor(LocalDate dueDate, LocalDate today) {
        return dueDate.isBefore(today) ? FinancialAlertType.OVERDUE : FinancialAlertType.UPCOMING;
    }

    static int group(FinancialAlertType type, LocalDate dueDate, LocalDate today) {
        if (type == FinancialAlertType.OVERDUE) return 0;
        return dueDate.equals(today) ? 1 : 2;
    }

    private NotificationResponse toResponse(FinancialNotification item) {
        FinancialEntry entry = item.getEntry();
        return new NotificationResponse(item.getId(), item.getAlertType().name(), entry.getId(), entry.getName(), entry.getAmount(), entry.getDueDate(), item.getCreatedAt(), item.getReadAt());
    }
}
