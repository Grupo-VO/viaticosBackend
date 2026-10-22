package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.Mantenimiento;
import com.grupovo.viaticos.domain.en.MantenimientoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Integer> {

    List<Mantenimiento> findAllByOrderByFechaDesc();

    List<Mantenimiento> findAllByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Mantenimiento> findAllByVehiculoIdVehiculoOrderByFechaDesc(Integer idVehiculo);

    boolean existsByVehiculoIdVehiculoAndEstado(Integer idVehiculo, MantenimientoEstado estado);
}
