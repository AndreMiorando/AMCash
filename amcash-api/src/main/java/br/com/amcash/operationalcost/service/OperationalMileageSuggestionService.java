package br.com.amcash.operationalcost.service;

import br.com.amcash.notification.dto.NotificationResponse;
import br.com.amcash.operationalcost.dto.response.MileageSuggestionsResponse;
import br.com.amcash.operationalcost.entity.*;
import br.com.amcash.operationalcost.repository.*;
import br.com.amcash.shared.exception.NotFoundException;
import br.com.amcash.vehicle.entity.OdometerReading;
import br.com.amcash.vehicle.repository.OdometerReadingRepository;
import br.com.amcash.work.entity.WorkDay;
import br.com.amcash.work.repository.WorkDayRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class OperationalMileageSuggestionService {
    private static final ZoneId BUSINESS_ZONE=ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal ABSOLUTE_THRESHOLD=new BigDecimal("300");
    private static final BigDecimal PERCENT_THRESHOLD=new BigDecimal("0.15");
    private final OperationalCostConfigurationRepository configurations;
    private final OperationalMileageNotificationRepository notifications;
    private final OdometerReadingRepository readings;
    private final WorkDayRepository workDays;

    public OperationalMileageSuggestionService(OperationalCostConfigurationRepository configurations,OperationalMileageNotificationRepository notifications,OdometerReadingRepository readings,WorkDayRepository workDays){this.configurations=configurations;this.notifications=notifications;this.readings=readings;this.workDays=workDays;}

    @Transactional public List<NotificationResponse> list(UUID userId){reconcile(userId);return notifications.findAllByUserIdAndActiveTrue(userId).stream().map(this::notificationResponse).toList();}
    @Transactional public List<NotificationResponse> markAllRead(UUID userId){reconcile(userId);OffsetDateTime now=OffsetDateTime.now(ZoneOffset.UTC);List<OperationalMileageNotification> current=notifications.findAllByUserIdAndActiveTrue(userId);current.forEach(item->item.markRead(now));return current.stream().map(this::notificationResponse).toList();}
    @Transactional(readOnly=true) public MileageSuggestionsResponse suggestions(UUID userId,UUID vehicleId){return response(find(userId,vehicleId),LocalDate.now(BUSINESS_ZONE));}
    @Transactional public void dismiss(UUID userId,UUID vehicleId,MileageSource source){OperationalCostConfiguration configuration=find(userId,vehicleId);notifications.findByConfigurationIdAndSuggestionSource(configuration.getId(),source).filter(OperationalMileageNotification::isActive).ifPresent(item->item.dismiss(LocalDate.now(BUSINESS_ZONE)));}
    @Transactional public void configurationChanged(UUID userId,UUID vehicleId,MileageSource source){OperationalCostConfiguration configuration=find(userId,vehicleId);for(OperationalMileageNotification item:notifications.findAllByConfigurationId(configuration.getId())){if(source!=MileageSource.MANUAL&&item.getSuggestionSource()==source)item.accept();else item.invalidate();}}

    @Scheduled(cron="0 20 0 * * *",zone="America/Sao_Paulo") @Transactional public void reconcileDaily(){configurations.findAll().stream().map(item->item.getUser().getId()).distinct().forEach(this::reconcile);}

    private void reconcile(UUID userId){LocalDate today=LocalDate.now(BUSINESS_ZONE);for(OperationalCostConfiguration configuration:configurations.findAllByUserIdOrderByUpdatedAtDesc(userId)){MileageSuggestionsResponse calculated=response(configuration,today);for(MileageSuggestionsResponse.Option option:calculated.options()){Optional<OperationalMileageNotification> stored=notifications.findByConfigurationIdAndSuggestionSource(configuration.getId(),option.source());if(!option.available()){stored.ifPresent(OperationalMileageNotification::invalidate);continue;}boolean relevant=shouldSuggest(configuration.getMonthlyMileage(),option.monthlyMileage());String evidence=evidence(option);if(stored.isEmpty()){if(relevant)notifications.save(new OperationalMileageNotification(configuration.getUser(),configuration,option.source(),option.monthlyMileage(),option.periodStart(),option.periodEnd(),evidence,today));continue;}OperationalMileageNotification tracker=stored.get();if(tracker.record(option.monthlyMileage(),option.periodStart(),option.periodEnd(),evidence,today,relevant)&&tracker.canNotify(today))tracker.activate();}}}

    private MileageSuggestionsResponse response(OperationalCostConfiguration configuration,LocalDate today){return new MileageSuggestionsResponse(configuration.getMonthlyMileage(),configuration.getMileageSource(),List.of(odometer(configuration,today),work(configuration,today)));}

    private MileageSuggestionsResponse.Option odometer(OperationalCostConfiguration configuration,LocalDate today){
        List<OdometerReading> ordered=readings.findAllByVehicleIdOrderByReadingDateDescCreatedAtDesc(configuration.getVehicle().getId()).stream().filter(item->!item.isCorrection()&&!item.getReadingDate().isAfter(today)).collect(java.util.stream.Collectors.toMap(OdometerReading::getReadingDate,item->item,(a,b)->a.getMileage().compareTo(b.getMileage())>=0?a:b,TreeMap::new)).values().stream().toList();
        if(ordered.size()<2)return unavailable(MileageSource.ODOMETER,"São necessárias leituras válidas cobrindo pelo menos 30 dias.");
        OdometerReading latest=ordered.getLast();if(latest.getReadingDate().isBefore(today.minusDays(30)))return unavailable(MileageSource.ODOMETER,"As leituras disponíveis não são recentes o suficiente.");
        BigDecimal kilometers=BigDecimal.ZERO;int days=0;LocalDate start=latest.getReadingDate();
        for(int index=ordered.size()-1;index>0&&days<30;index--){OdometerReading current=ordered.get(index);OdometerReading previous=ordered.get(index-1);long interval=ChronoUnit.DAYS.between(previous.getReadingDate(),current.getReadingDate());BigDecimal distance=current.getMileage().subtract(previous.getMileage());if(interval<=0||interval>90||distance.signum()<=0)continue;days+=Math.toIntExact(interval);kilometers=kilometers.add(distance);start=previous.getReadingDate();}
        if(days<30)return unavailable(MileageSource.ODOMETER,"São necessárias leituras válidas cobrindo pelo menos 30 dias.");
        BigDecimal estimate=kilometers.divide(BigDecimal.valueOf(days),8,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(30)).setScale(0,RoundingMode.HALF_UP);
        return new MileageSuggestionsResponse.Option(MileageSource.ODOMETER,estimate,start,latest.getReadingDate(),days,null);
    }

    private MileageSuggestionsResponse.Option work(OperationalCostConfiguration configuration,LocalDate today){LocalDate start=today.minusDays(29);UUID vehicleId=configuration.getVehicle().getId();if(!workDays.existsByVehicleIdAndDateLessThanEqual(vehicleId,start))return unavailable(MileageSource.WORK,"Ainda não há 30 dias de registros de Trabalho associados a este veículo.");List<WorkDay> items=workDays.findAllByVehicleIdAndDateBetweenOrderByDateDesc(vehicleId,start,today);BigDecimal total=items.stream().map(WorkDay::getKilometers).filter(Objects::nonNull).filter(value->value.signum()>=0).reduce(BigDecimal.ZERO,BigDecimal::add);return new MileageSuggestionsResponse.Option(MileageSource.WORK,total.setScale(0,RoundingMode.HALF_UP),start,today,30,null);}

    private MileageSuggestionsResponse.Option unavailable(MileageSource source,String reason){return new MileageSuggestionsResponse.Option(source,null,null,null,0,reason);}
    static boolean shouldSuggest(BigDecimal configured,BigDecimal suggested){BigDecimal difference=configured.subtract(suggested).abs();if(difference.compareTo(ABSOLUTE_THRESHOLD)>0)return true;return configured.signum()>0&&difference.divide(configured,8,RoundingMode.HALF_UP).compareTo(PERCENT_THRESHOLD)>0;}
    private String evidence(MileageSuggestionsResponse.Option option){String raw=option.source()+":"+option.monthlyMileage()+":"+option.periodStart()+":"+option.periodEnd()+":"+option.sampleDays();try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));return java.util.HexFormat.of().formatHex(digest);}catch(java.security.NoSuchAlgorithmException exception){throw new IllegalStateException(exception);}}
    private NotificationResponse notificationResponse(OperationalMileageNotification notification){OperationalCostConfiguration configuration=notification.getConfiguration();String vehicleName=configuration.getVehicle().getNickname()!=null?configuration.getVehicle().getNickname():configuration.getVehicle().getBrand()+" "+configuration.getVehicle().getModel();boolean odometer=notification.getSuggestionSource()==MileageSource.ODOMETER;String message=(odometer?"Odômetro: sua média estimada é de ":"Trabalho: sua média profissional estimada é de ")+notification.getSuggestedMileage().stripTrailingZeros().toPlainString()+" km/mês. Atualmente, o Custo Operacional considera "+configuration.getMonthlyMileage().stripTrailingZeros().toPlainString()+" km/mês.";return new NotificationResponse(notification.getId(),"MILEAGE_SUGGESTION",null,vehicleName,null,LocalDate.now(BUSINESS_ZONE),notification.getCreatedAt(),notification.getReadAt(),"OPERATIONAL_COST",configuration.getVehicle().getId(),message,notification.getSuggestionSource().name());}
    private OperationalCostConfiguration find(UUID userId,UUID vehicleId){return configurations.findByVehicleIdAndUserId(vehicleId,userId).orElseThrow(()->new NotFoundException("Configuração de custo operacional não encontrada"));}
}
