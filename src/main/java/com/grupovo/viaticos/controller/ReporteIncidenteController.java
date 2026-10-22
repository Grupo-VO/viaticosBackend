package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.ReporteIncidenteRequest;
import com.grupovo.viaticos.dto.ReporteIncidenteResponse;
import com.grupovo.viaticos.domain.en.ReporteIncidenteEstado;
import com.grupovo.viaticos.service.ReporteIncidenteService;
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
@RequestMapping("/api/v1/reportes")
public class ReporteIncidenteController {

    private final ReporteIncidenteService reporteIncidenteService;

    public ReporteIncidenteController(ReporteIncidenteService reporteIncidenteService) {
        this.reporteIncidenteService = reporteIncidenteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<List<ReporteIncidenteResponse>> listar() {
        return ResponseEntity.ok(reporteIncidenteService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<ReporteIncidenteResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(reporteIncidenteService.obtener(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<ReporteIncidenteResponse> crear(
            @RequestPart("request") @Valid ReporteIncidenteRequest request,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia) {
        return ResponseEntity.ok(reporteIncidenteService.crear(request, evidencia));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        reporteIncidenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO')")
    public ResponseEntity<ReporteIncidenteResponse> cambiarEstado(
            @PathVariable Integer id, @RequestBody ReporteIncidenteEstado estado) {
        return ResponseEntity.ok(reporteIncidenteService.cambiarEstado(id, estado));
    }

    @GetMapping("/{id}/evidencia")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO', 'CHOFER')")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable Integer id) {
        Resource resource = reporteIncidenteService.descargarEvidencia(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evidencia\"")
                .body(resource);
    }
}
