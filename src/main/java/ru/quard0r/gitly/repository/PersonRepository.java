package ru.quard0r.gitly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.quard0r.gitly.entity.Person;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByEmail(String email);

    Optional<Person> findByUsername(String username);

    Optional<Person> findByEmailOrUsername(String email, String username);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
