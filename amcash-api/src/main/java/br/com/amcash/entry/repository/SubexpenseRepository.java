package br.com.amcash.entry.repository;

import br.com.amcash.entry.entity.Subexpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubexpenseRepository extends JpaRepository<Subexpense, UUID> {

    List<Subexpense> findAllByEntryIdOrderByCreatedAtAsc(UUID entryId);

    Optional<Subexpense> findByIdAndEntryIdAndEntryUserId(UUID id, UUID entryId, UUID userId);

    List<Subexpense> findAllBySeriesIdAndEntryUserIdOrderByRecurrenceIndexAsc(UUID seriesId, UUID userId);
}
