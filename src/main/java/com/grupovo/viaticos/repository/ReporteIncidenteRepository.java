package com.grupovo.viaticos.repository;

import com.grupovo.viaticos.domain.ReporteIncidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteIncidenteRepository extends JpaRepository<ReporteIncidente, Integer> {

    List<ReporteIncidente> findAllByOrderByFechaDesc();

    List<ReporteIncidente> findAllByCreadoPorIdPersonaOrderByFechaDesc(Integer idPersona);

    List<ReporteIncidente> findAllByViajeIdViajeOrderByFechaDesc(Integer idViaje);
}
