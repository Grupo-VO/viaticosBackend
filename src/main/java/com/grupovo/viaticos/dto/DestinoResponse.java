package com.grupovo.viaticos.dto;

public record DestinoResponse(
        Integer idDestino,
        String nombre,
        String direccion,
        Double latitud,
        Double longitud,
        Boolean activo
) {
}
