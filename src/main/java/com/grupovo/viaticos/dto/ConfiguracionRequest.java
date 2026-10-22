package com.grupovo.viaticos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfiguracionRequest(
        @NotBlank String clave,
        @NotBlank String valor,
        String descripcion
) {
}
