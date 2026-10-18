package br.com.amcash.work.repository;

import br.com.amcash.work.entity.WorkEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface WorkEarningRepository extends JpaRepository<WorkEarning, UUID> {

    List<WorkEarning> findAllByWorkDayIdInOrderByWorkDayIdAscSortOrderAsc(Collection<UUID> workDayIds);

    @Modifying
    @Query("delete from WorkEarning earning where earning.workDay.id = :workDayId")
    void deleteAllByWorkDayId(@Param("workDayId") UUID workDayId);

    @Query("""
            select coalesce(sum(earning.amount), 0),
                   coalesce(sum(case when day.date = :referenceDate then earning.amount else 0 end), 0)
              from WorkEarning earning
              join earning.workDay day
             where day.user.id = :userId
               and day.date between :start and :end
            """)
    List<Object[]> summarizeMonth(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("referenceDate") LocalDate referenceDate);
}
