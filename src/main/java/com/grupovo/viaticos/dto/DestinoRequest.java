package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DestinoRequest(
        @NotBlank String nombre,
        @NotBlank String direccion,
        @NotNull Double latitud,
        @NotNull Double longitud
) {
}
