package br.com.amcash.vehicle.service;

import br.com.amcash.vehicle.entity.*; import br.com.amcash.vehicle.repository.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; import java.time.*; import java.util.*;

@Service
public class MaintenanceAlertService {
    private static final BigDecimal MILEAGE_NOTICE = new BigDecimal("500");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final VehicleMaintenanceRepository maintenanceRepository; private final MaintenanceAlertRepository alertRepository;
    public MaintenanceAlertService(VehicleMaintenanceRepository maintenanceRepository,MaintenanceAlertRepository alertRepository){this.maintenanceRepository=maintenanceRepository;this.alertRepository=alertRepository;}
    @Transactional public void reconcile(Vehicle vehicle){
        LocalDate today=LocalDate.now(BUSINESS_ZONE);
        for(VehicleMaintenance maintenance:maintenanceRepository.findAllByVehicleIdOrderByDateDescCreatedAtDesc(vehicle.getId())){
            MaintenanceAlertType type=typeFor(maintenance,vehicle.getCurrentMileage(),today);
            MaintenanceAlert alert=alertRepository.findByMaintenanceId(maintenance.getId()).orElse(null);
            if(type==null){if(alert!=null)alert.resolve();}
            else if(alert==null)alertRepository.save(new MaintenanceAlert(vehicle.getUser(),maintenance,type)); else alert.activate(type);
        }
    }
    @Transactional public void resolveCompleted(VehicleMaintenance completed){
        for(VehicleMaintenance previous:maintenanceRepository.findAllByVehicleIdOrderByDateDescCreatedAtDesc(completed.getVehicle().getId())){
            if(previous.getId().equals(completed.getId())||previous.getType()!=completed.getType()||previous.getDate().isAfter(completed.getDate()))continue;
            boolean reachedMileage=previous.getNextMileage()!=null&&completed.getMileage().compareTo(previous.getNextMileage())>=0;
            boolean reachedDate=previous.getNextDate()!=null&&!completed.getDate().isBefore(previous.getNextDate());
            if(reachedMileage||reachedDate)alertRepository.findByMaintenanceId(previous.getId()).ifPresent(MaintenanceAlert::resolve);
        }
    }
    static MaintenanceAlertType typeFor(VehicleMaintenance item,BigDecimal mileage,LocalDate today){
        boolean overdueMileage=item.getNextMileage()!=null&&mileage.compareTo(item.getNextMileage())>0;
        boolean overdueDate=item.getNextDate()!=null&&item.getNextDate().isBefore(today);
        if(overdueMileage||overdueDate)return MaintenanceAlertType.OVERDUE;
        boolean nearMileage=item.getNextMileage()!=null&&item.getNextMileage().subtract(mileage).compareTo(MILEAGE_NOTICE)<=0;
        boolean nearDate=item.getNextDate()!=null&&!item.getNextDate().isAfter(today.plusDays(7));
        return nearMileage||nearDate?MaintenanceAlertType.UPCOMING:null;
    }
}
