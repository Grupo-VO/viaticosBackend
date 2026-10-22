package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.ReporteIncidenteSeveridad;

public record IncidenteMapaResponse(
        Integer idReporte,
        String tipo,
        ReporteIncidenteSeveridad severidad,
        String descripcion,
        Double latitud,
        Double longitud
) {
}
