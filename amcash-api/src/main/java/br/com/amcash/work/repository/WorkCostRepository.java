package br.com.amcash.work.repository;

import br.com.amcash.work.entity.WorkCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkCostRepository extends JpaRepository<WorkCost, UUID> {

    List<WorkCost> findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(
            UUID userId, LocalDate start, LocalDate end);

    Optional<WorkCost> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select coalesce(sum(cost.amount), 0),
                   coalesce(sum(case when cost.date = :referenceDate then cost.amount else 0 end), 0)
              from WorkCost cost
             where cost.user.id = :userId
               and cost.date between :start and :end
            """)
    List<Object[]> summarizeMonth(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("referenceDate") LocalDate referenceDate);

    @Query("""
            select coalesce(sum(cost.amount), 0)
              from WorkCost cost
             where cost.user.id = :userId
               and cost.date between :start and :end
            """)
    BigDecimal sumByUserAndDateBetween(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
