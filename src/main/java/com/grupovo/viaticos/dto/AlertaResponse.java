package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.AlertaTipo;

import java.time.LocalDateTime;

public record AlertaResponse(
        Integer idAlerta,
        Integer idViaje,
        Integer idVehiculo,
        AlertaTipo tipo,
        String mensaje,
        LocalDateTime fecha,
        Boolean activa
) {
}
