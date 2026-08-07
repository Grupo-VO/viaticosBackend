package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    Optional<Persona> findByUsernameIgnoreCase(String username);
    Optional<Persona> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndIdPersonaNot(String username, Integer idPersona);
}
