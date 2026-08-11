package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.ReporteIncidenteTipo;

import java.time.LocalDateTime;

public record ReporteIncidenteResponse(
        Integer idReporte,
        Integer idViaje,
        String descripcionViaje,
        ReporteIncidenteTipo tipo,
        String descripcion,
        LocalDateTime fecha,
        Integer idCreadoPor,
        String nombreCreador,
        String evidenciaRuta,
        LocalDateTime fechaCreacion
) {
}
