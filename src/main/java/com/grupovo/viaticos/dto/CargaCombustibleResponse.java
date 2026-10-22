package com.grupovo.viaticos.dto;

import java.time.LocalDateTime;

public record CargaCombustibleResponse(
        Integer idCarga,
        Integer idVehiculo,
        String placaVehiculo,
        String vehiculoDescripcion,
        LocalDateTime fecha,
        Double litros,
        Double costoTotal,
        Double odometro,
        String comprobanteRuta,
        LocalDateTime fechaCreacion
) {
}
