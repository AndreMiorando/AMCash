package br.com.amcash.work.repository;

import br.com.amcash.work.entity.WorkDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkDayRepository extends JpaRepository<WorkDay, UUID> {

    List<WorkDay> findAllByUserIdAndDateBetweenOrderByDateDesc(UUID userId, LocalDate start, LocalDate end);

    Optional<WorkDay> findByUserIdAndDate(UUID userId, LocalDate date);
}
