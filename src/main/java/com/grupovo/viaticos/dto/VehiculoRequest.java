package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.VehiculoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record VehiculoRequest(
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotBlank String placa,
        @NotNull @PositiveOrZero Double kilometraje,
        @NotNull @Positive Double rendimiento,
        @NotNull @Positive Integer limitePasajeros,
        @NotNull @Positive Double capacidadTanqueLitros,
        @NotNull VehiculoStatus status
) {
}
