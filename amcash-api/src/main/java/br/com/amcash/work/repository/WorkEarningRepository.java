package br.com.amcash.work.repository;

import br.com.amcash.work.entity.WorkEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WorkEarningRepository extends JpaRepository<WorkEarning, UUID> {

    List<WorkEarning> findAllByWorkDayIdInOrderByWorkDayIdAscSortOrderAsc(Collection<UUID> workDayIds);

    @Modifying
    @Query("delete from WorkEarning earning where earning.workDay.id = :workDayId")
    void deleteAllByWorkDayId(@Param("workDayId") UUID workDayId);
}
