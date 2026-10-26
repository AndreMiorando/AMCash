package br.com.amcash.work.repository;

import br.com.amcash.work.entity.WorkEarningSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkEarningSourceRepository extends JpaRepository<WorkEarningSource, UUID> {

    List<WorkEarningSource> findAllByUserIdOrderBySortOrderAscNameAsc(UUID userId);

    Optional<WorkEarningSource> findByIdAndUserId(UUID id, UUID userId);

    Optional<WorkEarningSource> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndSystemKey(UUID userId, String systemKey);

    @Query("select coalesce(max(source.sortOrder), -1) from WorkEarningSource source where source.user.id = :userId")
    int maxSortOrder(@Param("userId") UUID userId);
}
