package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.Vehicle; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface VehicleRepository extends JpaRepository<Vehicle,UUID>{List<Vehicle> findAllByUserIdOrderByCreatedAtDesc(UUID userId);Optional<Vehicle> findByIdAndUserId(UUID id,UUID userId);Optional<Vehicle> findByUserIdAndInUseTrue(UUID userId);}
