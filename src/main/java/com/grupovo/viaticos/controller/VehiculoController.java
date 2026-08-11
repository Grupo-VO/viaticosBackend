package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.domain.en.VehiculoStatus;
import com.grupovo.viaticos.dto.VehiculoRequest;
import com.grupovo.viaticos.dto.VehiculoResponse;
import com.grupovo.viaticos.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> listar() {
        return ResponseEntity.ok(vehiculoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(vehiculoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity.ok(vehiculoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VehiculoResponse> actualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity.ok(vehiculoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VehiculoResponse> cambiarEstado(@PathVariable Integer id, @RequestBody Boolean activo) {
        return ResponseEntity.ok(vehiculoService.cambiarEstado(id, activo));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VehiculoResponse> cambiarStatus(@PathVariable Integer id,
                                                          @RequestBody VehiculoStatus status) {
        return ResponseEntity.ok(vehiculoService.cambiarStatus(id, status));
    }
}
