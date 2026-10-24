package br.com.amcash.operationalcost.repository;

import br.com.amcash.operationalcost.entity.OperationalMileageNotification;
import br.com.amcash.operationalcost.entity.MileageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface OperationalMileageNotificationRepository extends JpaRepository<OperationalMileageNotification,UUID> {
    List<OperationalMileageNotification> findAllByUserIdAndActiveTrue(UUID userId);
    List<OperationalMileageNotification> findAllByConfigurationId(UUID configurationId);
    Optional<OperationalMileageNotification> findByConfigurationIdAndSuggestionSource(UUID configurationId,MileageSource source);
    List<OperationalMileageNotification> findAllByConfigurationVehicleId(UUID vehicleId);
}
