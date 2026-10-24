package br.com.amcash.operationalcost.entity;

import br.com.amcash.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="operational_mileage_notifications",uniqueConstraints=@UniqueConstraint(name="uk_operational_mileage_configuration_source",columnNames={"configuration_id","suggestion_source"}))
public class OperationalMileageNotification {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="configuration_id",nullable=false) private OperationalCostConfiguration configuration;
    @Enumerated(EnumType.STRING) @Column(name="suggestion_source",nullable=false,length=16) private MileageSource suggestionSource;
    @Column(name="suggested_mileage",nullable=false,precision=12,scale=2) private BigDecimal suggestedMileage;
    @Column(name="period_start",nullable=false) private LocalDate periodStart;
    @Column(name="period_end",nullable=false) private LocalDate periodEnd;
    @Column(name="evaluated_at",nullable=false) private LocalDate evaluatedAt;
    @Column(name="evidence_key",nullable=false,length=64) private String evidenceKey;
    @Column(name="consecutive_relevant",nullable=false) private int consecutiveRelevant;
    @Column(name="rejection_count",nullable=false) private int rejectionCount;
    @Column(name="next_notification_at") private LocalDate nextNotificationAt;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="read_at") private OffsetDateTime readAt;
    @Column(nullable=false) private boolean active;

    protected OperationalMileageNotification() {}
    public OperationalMileageNotification(User user,OperationalCostConfiguration configuration,MileageSource source,BigDecimal mileage,LocalDate start,LocalDate end,String key,LocalDate today){id=UUID.randomUUID();this.user=user;this.configuration=configuration;suggestionSource=source;rejectionCount=0;active=false;createdAt=OffsetDateTime.now(ZoneOffset.UTC);record(mileage,start,end,key,today,true);}
    public boolean record(BigDecimal mileage,LocalDate start,LocalDate end,String key,LocalDate today,boolean relevant){if(key.equals(evidenceKey))return false;suggestedMileage=mileage;periodStart=start;periodEnd=end;evaluatedAt=today;evidenceKey=key;consecutiveRelevant=relevant?consecutiveRelevant+1:0;if(!relevant)active=false;return true;}
    public boolean canNotify(LocalDate today){return consecutiveRelevant>=2&&!active&&(nextNotificationAt==null||!today.isBefore(nextNotificationAt));}
    public void activate(){createdAt=OffsetDateTime.now(ZoneOffset.UTC);readAt=null;active=true;}
    public void dismiss(LocalDate today){active=false;readAt=null;consecutiveRelevant=0;rejectionCount++;nextNotificationAt=today.plusDays(rejectionCount>=3?90:30);}
    public void accept(){active=false;readAt=null;consecutiveRelevant=0;rejectionCount=0;nextNotificationAt=null;}
    public void invalidate(){active=false;readAt=null;consecutiveRelevant=0;}
    public void markRead(OffsetDateTime value){if(readAt==null)readAt=value;}
    public UUID getId(){return id;} public OperationalCostConfiguration getConfiguration(){return configuration;} public MileageSource getSuggestionSource(){return suggestionSource;} public BigDecimal getSuggestedMileage(){return suggestedMileage;} public LocalDate getPeriodStart(){return periodStart;} public LocalDate getPeriodEnd(){return periodEnd;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getReadAt(){return readAt;} public boolean isActive(){return active;}
}
