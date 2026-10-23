package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.VehicleFinance; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface VehicleFinanceRepository extends JpaRepository<VehicleFinance,UUID>{}
