package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.VehicleFinance; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface VehicleFinanceRepository extends JpaRepository<VehicleFinance,UUID>{@Modifying @Query("delete from VehicleFinance f where f.vehicleId=:vehicleId") void deleteAllByVehicleId(@Param("vehicleId") UUID vehicleId);}
