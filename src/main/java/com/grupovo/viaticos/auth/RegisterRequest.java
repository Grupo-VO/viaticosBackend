package com.grupovo.viaticos.auth;

import com.grupovo.viaticos.domain.en.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String nombre,
        @NotBlank String apellidoPaterno,
        @NotBlank String apellidoMaterno,
        @NotNull Role role,
        @NotBlank String username,
        @NotBlank String password
        ) {
}
