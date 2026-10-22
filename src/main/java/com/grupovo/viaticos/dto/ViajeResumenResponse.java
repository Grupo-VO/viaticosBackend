package com.grupovo.viaticos.dto;

import java.util.List;

public record ViajeResumenResponse(
        ViajeResponse viaje,
        List<UbicacionVehiculoResponse> ubicaciones,
        List<AlertaResponse> alertas,
        List<IncidenteMapaResponse> incidentes
) {
}
