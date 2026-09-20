package br.com.amcash.user.repository;

import br.com.amcash.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleSubject(String googleSubject);
}
