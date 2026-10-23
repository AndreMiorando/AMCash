package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.MaintenanceAlert; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface MaintenanceAlertRepository extends JpaRepository<MaintenanceAlert,UUID>{List<MaintenanceAlert> findAllByUserIdAndActiveTrue(UUID userId);Optional<MaintenanceAlert> findByMaintenanceId(UUID maintenanceId);}
