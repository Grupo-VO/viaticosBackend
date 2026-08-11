package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.ReporteIncidenteRequest;
import com.grupovo.viaticos.dto.ReporteIncidenteResponse;
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
    public ResponseEntity<List<ReporteIncidenteResponse>> listar() {
        return ResponseEntity.ok(reporteIncidenteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReporteIncidenteResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(reporteIncidenteService.obtener(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReporteIncidenteResponse> crear(
            @RequestPart("request") @Valid ReporteIncidenteRequest request,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia) {
        return ResponseEntity.ok(reporteIncidenteService.crear(request, evidencia));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        reporteIncidenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/evidencia")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable Integer id) {
        Resource resource = reporteIncidenteService.descargarEvidencia(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evidencia\"")
                .body(resource);
    }
}
