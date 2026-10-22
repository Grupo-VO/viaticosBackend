package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.RutaRequest;
import com.grupovo.viaticos.dto.RutaResponse;
import com.grupovo.viaticos.dto.RutaTrazoRequest;
import com.grupovo.viaticos.service.RutaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rutas")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping
    public ResponseEntity<List<RutaResponse>> listar() {
        return ResponseEntity.ok(rutaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RutaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(rutaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RutaResponse> crear(@Valid @RequestBody RutaRequest request) {
        return ResponseEntity.ok(rutaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RutaResponse> actualizar(@PathVariable Integer id,
                                                   @Valid @RequestBody RutaRequest request) {
        return ResponseEntity.ok(rutaService.actualizar(id, request));
    }

    @PostMapping("/{id}/refrescar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RutaResponse> refrescar(@PathVariable Integer id) {
        return ResponseEntity.ok(rutaService.refrescar(id));
    }

    @PatchMapping("/{id}/trazo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RutaResponse> actualizarTrazo(@PathVariable Integer id,
                                                        @Valid @RequestBody RutaTrazoRequest request) {
        return ResponseEntity.ok(rutaService.actualizarTrazo(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id, @RequestBody Boolean activo) {
        rutaService.cambiarEstado(id, activo);
        return ResponseEntity.noContent().build();
    }
}
