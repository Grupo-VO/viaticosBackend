package com.grupovo.viaticos.dto;

import java.util.List;

public record CasetaResponse(
        Integer idCaseta,
        String nombre,
        Double costo,
        Integer orden,
        Double latitud,
        Double longitud
) {
}
