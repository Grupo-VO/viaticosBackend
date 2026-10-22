package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RutaRequest(
        @NotBlank String nombre,
        @NotNull Integer idOrigen,
        @NotNull Integer idDestino,
        @NotNull Boolean usaCasetas,
        @NotNull @PositiveOrZero Double costoComida,
        @NotNull @PositiveOrZero Double costoHospedaje
) {
}
