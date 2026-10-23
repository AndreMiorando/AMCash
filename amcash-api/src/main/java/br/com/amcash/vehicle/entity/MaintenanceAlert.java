package br.com.amcash.vehicle.entity;

import br.com.amcash.user.entity.User; import jakarta.persistence.*; import java.time.*; import java.util.UUID;

@Entity @Table(name="vehicle_maintenance_alerts")
public class MaintenanceAlert {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="maintenance_id",nullable=false,unique=true) private VehicleMaintenance maintenance;
    @Enumerated(EnumType.STRING) @Column(name="alert_type",nullable=false,length=24) private MaintenanceAlertType type;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="read_at") private OffsetDateTime readAt;
    @Column(nullable=false) private boolean active;
    protected MaintenanceAlert(){}
    public MaintenanceAlert(User user,VehicleMaintenance maintenance,MaintenanceAlertType type){id=UUID.randomUUID();this.user=user;this.maintenance=maintenance;this.type=type;createdAt=OffsetDateTime.now(ZoneOffset.UTC);active=true;}
    public void activate(MaintenanceAlertType type){if(!active||this.type!=type){this.type=type;createdAt=OffsetDateTime.now(ZoneOffset.UTC);readAt=null;}active=true;}
    public void resolve(){active=false;} public void markRead(OffsetDateTime now){if(readAt==null)readAt=now;}
    public UUID getId(){return id;} public VehicleMaintenance getMaintenance(){return maintenance;} public MaintenanceAlertType getType(){return type;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getReadAt(){return readAt;}
}
