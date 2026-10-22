package com.grupovo.viaticos.dto;

import java.time.LocalDateTime;

public record UbicacionVehiculoResponse(
        Integer idUbicacion,
        Integer idViaje,
        Integer idVehiculo,
        Double latitud,
        Double longitud,
        Double velocidadKmh,
        LocalDateTime fecha
) {
}
