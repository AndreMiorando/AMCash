package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.VehiclePhoto; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.UUID;
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto,UUID>{@Modifying @Query("delete from VehiclePhoto p where p.vehicleId=:vehicleId") void deleteAllByVehicleId(@Param("vehicleId") UUID vehicleId);}
