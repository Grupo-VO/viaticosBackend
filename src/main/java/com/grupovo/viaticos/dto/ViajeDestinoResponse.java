package com.grupovo.viaticos.dto;

public record ViajeDestinoResponse(
        Integer idViajeDestino,
        String nombre,
        String direccion,
        Double latitud,
        Double longitud,
        Integer orden,
        Boolean visitado,
        String fechaLlegada
) {
}
