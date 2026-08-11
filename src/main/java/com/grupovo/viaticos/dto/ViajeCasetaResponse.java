package com.grupovo.viaticos.dto;

public record ViajeCasetaResponse(
        Integer idViajeCaseta,
        String nombre,
        Double costo,
        Integer orden,
        Double latitud,
        Double longitud
) {
}
