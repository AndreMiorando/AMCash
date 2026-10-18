package br.com.amcash.goal.repository;

import br.com.amcash.goal.entity.CustomGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomGoalRepository extends JpaRepository<CustomGoal, UUID> {
    Optional<CustomGoal> findByUserId(UUID userId);
    Optional<CustomGoal> findByUserIdAndTrackedOnHomeTrue(UUID userId);
    boolean existsByUserId(UUID userId);
}
