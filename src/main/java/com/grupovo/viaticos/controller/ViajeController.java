package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.ViajeRequest;
import com.grupovo.viaticos.dto.ViajeResumenResponse;
import com.grupovo.viaticos.dto.ViajeResponse;
import com.grupovo.viaticos.service.MonitoreoService;
import com.grupovo.viaticos.service.ViajeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/viajes")
public class ViajeController {

    private final ViajeService viajeService;
    private final MonitoreoService monitoreoService;

    public ViajeController(ViajeService viajeService, MonitoreoService monitoreoService) {
        this.viajeService = viajeService;
        this.monitoreoService = monitoreoService;
    }

    @GetMapping
    public ResponseEntity<List<ViajeResponse>> listar() {
        return ResponseEntity.ok(viajeService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViajeResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(viajeService.obtener(id));
    }

    @GetMapping("/{id}/resumen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<ViajeResumenResponse> resumen(@PathVariable Integer id) {
        return ResponseEntity.ok(monitoreoService.resumen(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ViajeResponse> crear(@Valid @RequestBody ViajeRequest request) {
        return ResponseEntity.ok(viajeService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO')")
    public ResponseEntity<ViajeResponse> actualizar(@PathVariable Integer id,
                                                    @Valid @RequestBody ViajeRequest request) {
        return ResponseEntity.ok(viajeService.actualizar(id, request));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO')")
    public ResponseEntity<ViajeResponse> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(viajeService.cancelar(id));
    }

    @PostMapping("/{id}/completar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO')")
    public ResponseEntity<ViajeResponse> completar(@PathVariable Integer id) {
        return ResponseEntity.ok(viajeService.completar(id));
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<ViajeResponse> iniciar(@PathVariable Integer id) {
        return ResponseEntity.ok(viajeService.iniciar(id));
    }

    @GetMapping("/activo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<ViajeResponse> viajeActivo() {
        return ResponseEntity.ok(viajeService.viajeActivo());
    }
}
