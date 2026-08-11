package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CargaCombustibleRequest(
        @NotNull Integer idVehiculo,
        @NotNull LocalDateTime fecha,
        @NotNull @Min(1) Double litros,
        @NotNull @Min(0) Double odometro
) {
}
