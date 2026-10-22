package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.MantenimientoRequest;
import com.grupovo.viaticos.dto.MantenimientoResponse;
import com.grupovo.viaticos.service.MantenimientoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    public MantenimientoController(MantenimientoService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'CHOFER')")
    public ResponseEntity<List<MantenimientoResponse>> listar() {
        return ResponseEntity.ok(mantenimientoService.listar());
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    public ResponseEntity<List<MantenimientoResponse>> listarPorVehiculo(@PathVariable Integer idVehiculo) {
        return ResponseEntity.ok(mantenimientoService.listarPorVehiculo(idVehiculo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MantenimientoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(mantenimientoService.obtener(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CHOFER')")
    public ResponseEntity<MantenimientoResponse> crear(
            @RequestPart("request") @Valid MantenimientoRequest request,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia) {
        return ResponseEntity.ok(mantenimientoService.crear(request, evidencia));
    }

    @PatchMapping("/{id}/completar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MantenimientoResponse> completar(@PathVariable Integer id) {
        return ResponseEntity.ok(mantenimientoService.completar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        mantenimientoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/evidencia")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable Integer id) {
        Resource resource = mantenimientoService.descargarEvidencia(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evidencia\"")
                .body(resource);
    }
}
