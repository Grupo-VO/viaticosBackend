package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.Caseta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CasetaRepository extends JpaRepository<Caseta, Integer> {
}
