package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RutaRequest(
        @NotBlank String nombre,
        @NotNull Integer idOrigen,
        @NotNull Integer idDestino,
        @NotNull Boolean usaCasetas,
        @NotNull @Min(0) Double costoComida,
        @NotNull @Min(0) Double costoHospedaje
) {
}
