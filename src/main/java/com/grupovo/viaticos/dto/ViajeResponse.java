package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.TipoTrayecto;
import com.grupovo.viaticos.domain.en.ViajeStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ViajeResponse(
        Integer idViaje,
        Integer idRuta,
        String nombreRuta,
        String origenRuta,
        String destinoRuta,
        Integer idVehiculo,
        String placaVehiculo,
        String vehiculoDescripcion,
        Integer idChofer,
        String nombreChofer,
        Integer numeroPasajeros,
        Integer limitePasajeros,
        Boolean excesoCapacidad,
        LocalDateTime fechaSalida,
        LocalDateTime fechaRegreso,
        TipoTrayecto tipoTrayecto,
        ViajeStatus status,
        Integer totalDias,
        Double totalCasetas,
        Double totalViaticos,
        Double totalViaje,
        Double litrosConsumidos,
        Double costoCombustible,
        Boolean alertaGasolinaBaja,
        Double distanciaKm,
        Double duracionMinutos,
        Double costoComida,
        Double costoHospedaje,
        String polylineCasetas,
        String polylineLibre,
        String polylineCasetasIda,
        String polylineCasetasRegreso,
        String polylineLibreIda,
        String polylineLibreRegreso,
        List<ViajeDestinoResponse> destinos,
        List<ViajeCasetaResponse> casetas,
        LocalDateTime fechaCreacion,
        Double nivelCombustibleActual,
        Double combustibleRequerido,
        Double combustibleFaltante
) {
}
