package br.com.amcash.vehicle.repository;
import br.com.amcash.vehicle.entity.VehiclePhoto; import org.springframework.data.jpa.repository.JpaRepository; import java.util.UUID;
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto,UUID>{}
