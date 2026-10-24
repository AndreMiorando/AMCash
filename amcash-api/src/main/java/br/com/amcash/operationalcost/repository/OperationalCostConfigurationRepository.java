package br.com.amcash.operationalcost.repository;
import br.com.amcash.operationalcost.entity.OperationalCostConfiguration; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface OperationalCostConfigurationRepository extends JpaRepository<OperationalCostConfiguration,UUID>{List<OperationalCostConfiguration> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);Optional<OperationalCostConfiguration> findByVehicleIdAndUserId(UUID vehicleId,UUID userId);boolean existsByVehicleId(UUID vehicleId);}
