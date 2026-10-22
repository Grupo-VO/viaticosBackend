package com.grupovo.viaticos.auth;

public record AuthResponse(
        String usuario,
        String nombre,
        String role
) {
}
