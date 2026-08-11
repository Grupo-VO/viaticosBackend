package com.grupovo.viaticos.dto;

import com.grupovo.viaticos.domain.en.TipoTrayecto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ViajeRequest(
        @NotNull Integer idOrigen,
        @NotEmpty List<Integer> idDestinos,
        @NotNull Integer idVehiculo,
        @NotNull Integer idChofer,
        @NotNull @Min(1) Integer numeroPasajeros,
        @NotNull LocalDateTime fechaSalida,
        @NotNull LocalDateTime fechaRegreso,
        @NotNull TipoTrayecto tipoTrayecto,
        Boolean optimizar,
        @NotNull @DecimalMin("0.0") Double costoComida,
        @NotNull @DecimalMin("0.0") Double costoHospedaje
) {
}
