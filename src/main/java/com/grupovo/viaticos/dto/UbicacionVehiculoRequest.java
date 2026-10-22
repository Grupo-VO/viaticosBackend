package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UbicacionVehiculoRequest(
        @NotNull Integer idViaje,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
        @DecimalMin("0.0") Double velocidadKmh,
        LocalDateTime fecha
) {
}
