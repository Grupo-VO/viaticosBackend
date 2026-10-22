package com.grupovo.viaticos.dto;

import java.util.List;

public record RutaResponse(
        Integer idRuta,
        String nombre,
        DestinoResponse origen,
        DestinoResponse destino,
        Boolean usaCasetas,
        Double duracionMinutos,
        Double distanciaKm,
        Double costoComida,
        Double costoHospedaje,
        Double costoTotalCasetas,
        String polylineCasetas,
        String polylineLibre,
        String polylineCasetasIda,
        String polylineCasetasRegreso,
        String polylineLibreIda,
        String polylineLibreRegreso,
        List<CasetaResponse> casetas,
        Boolean activo
) {
}
