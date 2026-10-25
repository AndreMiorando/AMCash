package br.com.amcash.operationalcost.entity;

import br.com.amcash.work.entity.WorkCostCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Embeddable
public class OperationalCustomFixedCost {
    @Column(nullable=false,length=60) private String title;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private CostPeriod period;
    @Enumerated(EnumType.STRING) @Column(name="work_cost_category",length=24) private WorkCostCategory workCostCategory;
    protected OperationalCustomFixedCost() {}
    public OperationalCustomFixedCost(String title,BigDecimal amount,CostPeriod period,WorkCostCategory workCostCategory){this.title=title;this.amount=amount;this.period=period;this.workCostCategory=workCostCategory;}
    public String getTitle(){return title;} public BigDecimal getAmount(){return amount;} public CostPeriod getPeriod(){return period;} public WorkCostCategory getWorkCostCategory(){return workCostCategory;}
}
