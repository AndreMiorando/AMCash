package br.com.amcash.user.repository;

import br.com.amcash.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByIdAndDeletionRequestedAtIsNotNull(UUID id);

    @Modifying
    @Query("delete from User u where u.deletionRequestedAt is not null and u.deletionRequestedAt <= :limit")
    int deleteAccountsRequestedBefore(@Param("limit") OffsetDateTime limit);
}
