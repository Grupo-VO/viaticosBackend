package com.grupovo.viaticos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MonitoreoViajeResponse(
        Integer idViaje,
        Integer idVehiculo,
        String placaVehiculo,
        String vehiculoDescripcion,
        Integer idChofer,
        String nombreChofer,
        String origenRuta,
        String destinoRuta,
        Double ultimaLatitud,
        Double ultimaLongitud,
        Double velocidadKmh,
        LocalDateTime ultimaFechaUbicacion,
        Integer destinosTotales,
        Integer destinosVisitados,
        List<ViajeDestinoResponse> destinos,
        List<AlertaResponse> alertasActivas,
        Double nivelCombustible,
        Double capacidadTanqueLitros,
        LocalDateTime fechaSalida
) {
}
