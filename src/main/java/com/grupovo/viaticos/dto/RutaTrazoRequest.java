package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RutaTrazoRequest(
        @NotBlank String polyline,
        @PositiveOrZero Double distanciaKm,
        @PositiveOrZero Double duracionMinutos,
        @NotBlank String tipo,
        String polylineIda,
        String polylineRegreso
) {
}
