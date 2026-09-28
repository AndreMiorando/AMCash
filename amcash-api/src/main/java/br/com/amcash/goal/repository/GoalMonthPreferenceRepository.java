package br.com.amcash.goal.repository;

import br.com.amcash.goal.entity.GoalMonthPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoalMonthPreferenceRepository extends JpaRepository<GoalMonthPreference, UUID> {

    Optional<GoalMonthPreference> findByUserIdAndYearAndMonth(UUID userId, int year, int month);
}
