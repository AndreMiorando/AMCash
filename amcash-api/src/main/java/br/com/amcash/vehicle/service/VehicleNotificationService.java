package br.com.amcash.vehicle.service;

import br.com.amcash.notification.dto.NotificationResponse; import br.com.amcash.vehicle.entity.*; import br.com.amcash.vehicle.repository.*;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; import java.time.*; import java.util.*;

@Service
public class VehicleNotificationService {
    private static final ZoneId BUSINESS_ZONE=ZoneId.of("America/Sao_Paulo");
    private final VehicleRepository vehicles; private final MaintenanceAlertRepository repository; private final MaintenanceAlertService alerts; private final MileageReminderService mileageReminders;
    public VehicleNotificationService(VehicleRepository vehicles,MaintenanceAlertRepository repository,MaintenanceAlertService alerts,MileageReminderService mileageReminders){this.vehicles=vehicles;this.repository=repository;this.alerts=alerts;this.mileageReminders=mileageReminders;}
    @Transactional public List<NotificationResponse> list(UUID userId){reconcile(vehicles.findAllByUserIdOrderByCreatedAtDesc(userId));return combined(repository.findAllByUserIdAndActiveTrue(userId).stream().map(this::response).toList(),mileageReminders.list(userId));}
    @Transactional public List<NotificationResponse> markAllRead(UUID userId){reconcile(vehicles.findAllByUserIdOrderByCreatedAtDesc(userId));OffsetDateTime now=OffsetDateTime.now(ZoneOffset.UTC);List<MaintenanceAlert> current=repository.findAllByUserIdAndActiveTrue(userId);current.forEach(a->a.markRead(now));return combined(current.stream().map(this::response).toList(),mileageReminders.markAllRead(userId));}
    @Scheduled(cron="0 15 0 * * *",zone="America/Sao_Paulo") @Transactional public void reconcileDaily(){reconcile(vehicles.findAll());}
    private void reconcile(List<Vehicle> current){current.forEach(vehicle->{alerts.reconcile(vehicle);mileageReminders.reconcile(vehicle);});}
    private List<NotificationResponse> combined(List<NotificationResponse> maintenance,List<NotificationResponse> mileage){return java.util.stream.Stream.concat(maintenance.stream(),mileage.stream()).toList();}
    private NotificationResponse response(MaintenanceAlert alert){VehicleMaintenance m=alert.getMaintenance();Vehicle v=m.getVehicle();String name=m.getCustomName()!=null?m.getCustomName():label(m.getType());String message=message(m,v.getCurrentMileage());LocalDate due=m.getNextDate()!=null?m.getNextDate():LocalDate.now(BUSINESS_ZONE);return new NotificationResponse(alert.getId(),alert.getType().name(),null,name,null,due,alert.getCreatedAt(),alert.getReadAt(),"VEHICLE",v.getId(),message);}
    private String message(VehicleMaintenance m,BigDecimal mileage){if(m.getNextMileage()!=null&&mileage.compareTo(m.getNextMileage())>=0)return "Revisão prevista aos "+m.getNextMileage().stripTrailingZeros().toPlainString()+" km";if(m.getNextDate()!=null)return "Revisão prevista para "+m.getNextDate();return "Revisão próxima";}
    private String label(MaintenanceType type){return switch(type){case OIL_CHANGE->"Troca de óleo";case FILTERS->"Filtros";case TIRES->"Pneus";case BRAKES->"Freios";case INSPECTION->"Revisão";case ALIGNMENT_BALANCING->"Alinhamento e balanceamento";case OTHER->"Outra manutenção";};}
}
