package br.com.amcash.vehicle.repository;

import br.com.amcash.vehicle.entity.VehicleMileageReminder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface VehicleMileageReminderRepository extends JpaRepository<VehicleMileageReminder, UUID> {
    List<VehicleMileageReminder> findAllByUserIdAndActiveTrue(UUID userId);
    Optional<VehicleMileageReminder> findByVehicleId(UUID vehicleId);
    @Modifying @Query("delete from VehicleMileageReminder r where r.vehicle.id=:vehicleId")
    void deleteAllByVehicleId(@Param("vehicleId") UUID vehicleId);
}
