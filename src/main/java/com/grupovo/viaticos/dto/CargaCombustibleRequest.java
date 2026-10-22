package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record CargaCombustibleRequest(
        @NotNull Integer idVehiculo,
        @NotNull LocalDateTime fecha,
        @NotNull @Positive Double litros,
        @NotNull @PositiveOrZero Double odometro
) {
}
