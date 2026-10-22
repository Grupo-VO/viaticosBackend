package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.CargaCombustible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CargaCombustibleRepository extends JpaRepository<CargaCombustible, Integer> {

    List<CargaCombustible> findAllByOrderByFechaDesc();

    List<CargaCombustible> findAllByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<CargaCombustible> findAllByVehiculoIdVehiculoOrderByFechaDesc(Integer idVehiculo);
}
