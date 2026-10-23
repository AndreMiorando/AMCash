package br.com.amcash.goal.repository;

import br.com.amcash.goal.entity.MonthlyGoalConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MonthlyGoalConfigurationRepository extends JpaRepository<MonthlyGoalConfiguration, UUID> {
    Optional<MonthlyGoalConfiguration> findByUserId(UUID userId);
}
