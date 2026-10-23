package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.OdometerReading; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface OdometerReadingRepository extends JpaRepository<OdometerReading,UUID>{List<OdometerReading> findAllByVehicleIdOrderByReadingDateDescCreatedAtDesc(UUID vehicleId);}
