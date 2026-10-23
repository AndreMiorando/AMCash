package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.VehicleMaintenance; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance,UUID>{List<VehicleMaintenance> findAllByVehicleIdOrderByDateDescCreatedAtDesc(UUID vehicleId);Optional<VehicleMaintenance> findByIdAndVehicleUserId(UUID id,UUID userId);}
