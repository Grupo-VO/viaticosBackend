package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.TipoTrayecto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ViajeRequest(
        @NotNull Integer idRuta,
        @NotNull Integer idVehiculo,
        @NotNull Integer idChofer,
        @NotNull @Min(1) Integer numeroPasajeros,
        @NotNull LocalDateTime fechaSalida,
        @NotNull LocalDateTime fechaRegreso,
        @NotNull TipoTrayecto tipoTrayecto
) {
}
