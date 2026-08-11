package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.VehiculoStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehiculoRequest(
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotBlank String placa,
        @NotNull Double kilometraje,
        @NotNull Double rendimiento,
        @NotNull @Min(1) Integer limitePasajeros,
        @NotNull @Min(1) Double capacidadTanqueLitros,
        @NotNull VehiculoStatus status
) {
}
