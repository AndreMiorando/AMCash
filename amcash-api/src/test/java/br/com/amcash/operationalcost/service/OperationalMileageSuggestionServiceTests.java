package br.com.amcash.operationalcost.service;

import br.com.amcash.operationalcost.entity.*;
import br.com.amcash.operationalcost.repository.*;
import br.com.amcash.user.entity.User;
import br.com.amcash.vehicle.entity.*;
import br.com.amcash.vehicle.repository.OdometerReadingRepository;
import br.com.amcash.work.entity.WorkDay;
import br.com.amcash.work.repository.WorkDayRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationalMileageSuggestionServiceTests {
    @Test void shouldSuggestWhenDifferenceExceedsFifteenPercent(){assertTrue(OperationalMileageSuggestionService.shouldSuggest(new BigDecimal("3000"),new BigDecimal("3451")));}
    @Test void shouldSuggestWhenAbsoluteDifferenceExceedsThreeHundredKilometers(){assertTrue(OperationalMileageSuggestionService.shouldSuggest(new BigDecimal("10000"),new BigDecimal("10301")));}
    @Test void shouldNotSuggestAtTheExactLimits(){assertFalse(OperationalMileageSuggestionService.shouldSuggest(new BigDecimal("2000"),new BigDecimal("2300")));}

    @Test void shouldUseWeightedValidOdometerIntervalsOverAtLeastThirtyDays(){
        Fixture fixture=new Fixture();LocalDate today=LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        List<OdometerReading> history=List.of(reading(today,"2600"),reading(today.minusDays(20),"2000"),reading(today.minusDays(40),"1000"));
        when(fixture.readings.findAllByVehicleIdOrderByReadingDateDescCreatedAtDesc(fixture.vehicleId)).thenReturn(history);
        var option=fixture.service.suggestions(fixture.userId,fixture.vehicleId).options().getFirst();
        assertTrue(option.available());assertEquals(new BigDecimal("1200"),option.monthlyMileage());assertEquals(40,option.sampleDays());
    }

    @Test void shouldCountCalendarDaysAndOnlyWorkLinkedToTheVehicle(){
        Fixture fixture=new Fixture();LocalDate today=LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));LocalDate start=today.minusDays(29);
        when(fixture.workDays.existsByVehicleIdAndDateLessThanEqual(fixture.vehicleId,start)).thenReturn(true);
        WorkDay first=mock(WorkDay.class),second=mock(WorkDay.class);when(first.getKilometers()).thenReturn(new BigDecimal("800"));when(second.getKilometers()).thenReturn(new BigDecimal("700"));
        when(fixture.workDays.findAllByVehicleIdAndDateBetweenOrderByDateDesc(fixture.vehicleId,start,today)).thenReturn(List.of(first,second));
        var option=fixture.service.suggestions(fixture.userId,fixture.vehicleId).options().get(1);
        assertTrue(option.available());assertEquals(new BigDecimal("1500"),option.monthlyMileage());assertEquals(30,option.sampleDays());
    }

    private static OdometerReading reading(LocalDate date,String mileage){OdometerReading item=mock(OdometerReading.class);when(item.getReadingDate()).thenReturn(date);when(item.getMileage()).thenReturn(new BigDecimal(mileage));when(item.isCorrection()).thenReturn(false);return item;}
    private static class Fixture{
        final UUID userId=UUID.randomUUID(),vehicleId=UUID.randomUUID();final OperationalCostConfigurationRepository configurations=mock(OperationalCostConfigurationRepository.class);final OperationalMileageNotificationRepository notifications=mock(OperationalMileageNotificationRepository.class);final OdometerReadingRepository readings=mock(OdometerReadingRepository.class);final WorkDayRepository workDays=mock(WorkDayRepository.class);final OperationalMileageSuggestionService service=new OperationalMileageSuggestionService(configurations,notifications,readings,workDays);
        Fixture(){OperationalCostConfiguration configuration=mock(OperationalCostConfiguration.class);Vehicle vehicle=mock(Vehicle.class);User user=mock(User.class);when(vehicle.getId()).thenReturn(vehicleId);when(configuration.getVehicle()).thenReturn(vehicle);when(configuration.getUser()).thenReturn(user);when(configuration.getMonthlyMileage()).thenReturn(new BigDecimal("3000"));when(configuration.getMileageSource()).thenReturn(MileageSource.MANUAL);when(configurations.findByVehicleIdAndUserId(vehicleId,userId)).thenReturn(Optional.of(configuration));}
    }
}
