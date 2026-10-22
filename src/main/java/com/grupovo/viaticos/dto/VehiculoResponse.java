package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.VehiculoStatus;

import java.time.LocalDateTime;

public record VehiculoResponse(
        Integer idVehiculo,
        String marca,
        String modelo,
        String placa,
        Double kilometraje,
        Double rendimiento,
        Integer limitePasajeros,
        Double capacidadTanqueLitros,
        Double nivelCombustible,
        Boolean activo,
        VehiculoStatus status,
        LocalDateTime fechaCreacion
) {
}
