package br.com.amcash.entry.repository;

import br.com.amcash.entry.entity.FinancialEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, UUID> {

    List<FinancialEntry> findAllByUserIdAndDueDateBetweenOrderByDueDateAscCreatedAtAsc(
            UUID userId,
            LocalDate start,
            LocalDate end);

    Optional<FinancialEntry> findByIdAndUserId(UUID id, UUID userId);

    List<FinancialEntry> findAllBySeriesIdAndUserIdOrderByRecurrenceIndexAsc(UUID seriesId, UUID userId);

    @Query("""
            select entry.seriesId as seriesId,
                   count(entry) as totalOccurrences,
                   sum(case when entry.paid = true then 1 else 0 end) as paidOccurrences
            from FinancialEntry entry
            where entry.user.id = :userId and entry.seriesId in :seriesIds
            group by entry.seriesId
            """)
    List<SeriesStatistics> summarizeSeries(
            @Param("userId") UUID userId,
            @Param("seriesIds") Collection<UUID> seriesIds);

    interface SeriesStatistics {
        UUID getSeriesId();
        long getTotalOccurrences();
        long getPaidOccurrences();
    }
}
