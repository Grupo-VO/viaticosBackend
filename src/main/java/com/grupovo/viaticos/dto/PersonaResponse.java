package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.Role;

import java.time.LocalDateTime;

public record PersonaResponse(
        Integer idPersona,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String username,
        Role role,
        Boolean activa,
        String foto,
        LocalDateTime fechaCreacion
) {
}
