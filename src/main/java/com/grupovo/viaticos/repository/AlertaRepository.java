package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.Alerta;
import com.grupovo.viaticos.domain.en.AlertaTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    boolean existsByViajeIdViajeAndTipoAndActiva(Integer idViaje, AlertaTipo tipo, boolean activa);

    List<Alerta> findByViajeIdViajeAndTipoAndActiva(Integer idViaje, AlertaTipo tipo, boolean activa);

    List<Alerta> findByViajeIdViajeOrderByFechaDesc(Integer idViaje);

    List<Alerta> findByViajeIdViajeAndActivaOrderByFechaDesc(Integer idViaje, boolean activa);

    List<Alerta> findByViajeIdViajeInAndActivaOrderByFechaDesc(java.util.Collection<Integer> idViajes, boolean activa);
}
