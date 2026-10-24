package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.OdometerReading; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface OdometerReadingRepository extends JpaRepository<OdometerReading,UUID>{List<OdometerReading> findAllByVehicleIdOrderByReadingDateDescCreatedAtDesc(UUID vehicleId);@Modifying @Query("delete from OdometerReading r where r.vehicle.id=:vehicleId") void deleteAllByVehicleId(@Param("vehicleId") UUID vehicleId);}
