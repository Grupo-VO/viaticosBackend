package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.domain.Alerta;
import com.grupovo.viaticos.dto.AlertaResponse;
import com.grupovo.viaticos.dto.UbicacionVehiculoRequest;
import com.grupovo.viaticos.dto.UbicacionVehiculoResponse;
import com.grupovo.viaticos.service.UbicacionVehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ubicaciones")
public class UbicacionVehiculoController {

    private final UbicacionVehiculoService ubicacionService;

    public UbicacionVehiculoController(UbicacionVehiculoService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<UbicacionVehiculoResponse> registrar(@Valid @RequestBody UbicacionVehiculoRequest request) {
        return ResponseEntity.ok(ubicacionService.registrar(request));
    }

    @GetMapping("/viaje/{idViaje}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<List<UbicacionVehiculoResponse>> historial(@PathVariable Integer idViaje) {
        return ResponseEntity.ok(ubicacionService.historial(idViaje));
    }

    @GetMapping("/viaje/{idViaje}/alertas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<List<AlertaResponse>> alertas(@PathVariable Integer idViaje) {
        return ResponseEntity.ok(ubicacionService.alertasDelViaje(idViaje).stream()
                .map(a -> new AlertaResponse(
                        a.getIdAlerta(), a.getViaje().getIdViaje(), a.getVehiculo().getIdVehiculo(),
                        a.getTipo(), a.getMensaje(), a.getFecha(), a.getActiva()))
                .toList());
    }
}
