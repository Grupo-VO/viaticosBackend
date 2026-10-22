package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.ReporteIncidenteTipo;
import com.grupovo.viaticos.domain.en.ReporteIncidenteEstado;
import com.grupovo.viaticos.domain.en.ReporteIncidenteSeveridad;

import java.time.LocalDateTime;

public record ReporteIncidenteResponse(
        Integer idReporte,
        Integer idViaje,
        String descripcionViaje,
        ReporteIncidenteTipo tipo,
        ReporteIncidenteSeveridad severidad,
        ReporteIncidenteEstado estado,
        String descripcion,
        LocalDateTime fecha,
        Integer idCreadoPor,
        String nombreCreador,
        String evidenciaRuta,
        Double latitud,
        Double longitud,
        LocalDateTime fechaCreacion
) {
}
