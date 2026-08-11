package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.ReporteIncidenteTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReporteIncidenteRequest(
        Integer idViaje,
        @NotNull ReporteIncidenteTipo tipo,
        @NotBlank String descripcion,
        @NotNull LocalDateTime fecha
) {
}
