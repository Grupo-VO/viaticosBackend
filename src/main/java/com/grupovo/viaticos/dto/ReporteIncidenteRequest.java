package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.ReporteIncidenteTipo;
import com.grupovo.viaticos.domain.en.ReporteIncidenteSeveridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReporteIncidenteRequest(
        Integer idViaje,
        @NotNull ReporteIncidenteTipo tipo,
        @NotNull ReporteIncidenteSeveridad severidad,
        @NotBlank String descripcion,
        @NotNull LocalDateTime fecha,
        Double latitud,
        Double longitud
) {
}
