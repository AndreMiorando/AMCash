package br.com.amcash.entry.repository;

import br.com.amcash.entry.entity.FinancialEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
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
}
