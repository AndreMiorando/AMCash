package br.com.amcash.notification.repository;

import br.com.amcash.notification.entity.FinancialNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialNotificationRepository extends JpaRepository<FinancialNotification, UUID> {
    List<FinancialNotification> findAllByUserIdAndActiveTrue(UUID userId);
    List<FinancialNotification> findAllByActiveTrue();
    Optional<FinancialNotification> findByUserIdAndEntryId(UUID userId, UUID entryId);
}
