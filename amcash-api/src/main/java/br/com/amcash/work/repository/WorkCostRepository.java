package br.com.amcash.work.repository;

import br.com.amcash.work.entity.WorkCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkCostRepository extends JpaRepository<WorkCost, UUID> {

    List<WorkCost> findAllByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(
            UUID userId, LocalDate start, LocalDate end);

    Optional<WorkCost> findByIdAndUserId(UUID id, UUID userId);
}
