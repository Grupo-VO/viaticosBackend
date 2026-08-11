package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.MantenimientoEstado;
import com.grupovo.viaticos.domain.en.MantenimientoTipo;

import java.time.LocalDateTime;

public record MantenimientoResponse(
        Integer idMantenimiento,
        Integer idVehiculo,
        String placaVehiculo,
        String vehiculoDescripcion,
        MantenimientoTipo tipo,
        MantenimientoEstado estado,
        LocalDateTime fecha,
        String descripcion,
        Double costo,
        Double odometro,
        String evidenciaRuta,
        LocalDateTime fechaCreacion
) {
}
