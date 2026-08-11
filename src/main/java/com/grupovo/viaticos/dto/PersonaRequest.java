package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PersonaRequest(
        @NotBlank String nombre,
        @NotBlank String apellidoPaterno,
        @NotBlank String apellidoMaterno,
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Role role,
        Boolean activa
) {
}
