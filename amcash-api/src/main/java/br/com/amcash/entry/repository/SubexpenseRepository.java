package br.com.amcash.entry.repository;

import br.com.amcash.entry.entity.Subexpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubexpenseRepository extends JpaRepository<Subexpense, UUID> {

    List<Subexpense> findAllByEntryIdOrderByCreatedAtAsc(UUID entryId);

    List<Subexpense> findAllByEntryIdInOrderByEntryIdAscCreatedAtAsc(Collection<UUID> entryIds);

    boolean existsByEntryId(UUID entryId);

    Optional<Subexpense> findByIdAndEntryIdAndEntryUserId(UUID id, UUID entryId, UUID userId);

    List<Subexpense> findAllBySeriesIdAndEntryUserIdOrderByRecurrenceIndexAsc(UUID seriesId, UUID userId);

    @Query("""
            select item.entry.id as entryId, sum(item.amount) as total
            from Subexpense item
            where item.entry.id in :entryIds
            group by item.entry.id
            """)
    List<EntryTotal> sumAmountsByEntryIds(@Param("entryIds") Collection<UUID> entryIds);

    interface EntryTotal {
        UUID getEntryId();
        BigDecimal getTotal();
    }
}
