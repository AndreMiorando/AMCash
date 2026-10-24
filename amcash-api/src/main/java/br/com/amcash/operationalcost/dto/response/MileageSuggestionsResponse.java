package br.com.amcash.operationalcost.dto.response;

import br.com.amcash.operationalcost.entity.MileageSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MileageSuggestionsResponse(BigDecimal configuredMileage,MileageSource configuredSource,List<Option> options) {
    public record Option(MileageSource source,BigDecimal monthlyMileage,LocalDate periodStart,LocalDate periodEnd,int sampleDays,String unavailableReason) {
        public boolean available(){return monthlyMileage!=null;}
    }
}
