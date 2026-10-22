package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.MantenimientoTipo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record MantenimientoRequest(
        @NotNull Integer idVehiculo,
        @NotNull MantenimientoTipo tipo,
        @NotNull LocalDateTime fecha,
        @NotBlank String descripcion,
        @NotNull @Min(0) Double costo,
        @NotNull @Min(0) Double odometro
) {
}
