package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.CargaCombustibleRequest;
import com.grupovo.viaticos.dto.CargaCombustibleResponse;
import com.grupovo.viaticos.service.CargaCombustibleService;
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
@RequestMapping("/api/v1/cargas-combustible")
public class CargaCombustibleController {

    private final CargaCombustibleService cargaCombustibleService;

    public CargaCombustibleController(CargaCombustibleService cargaCombustibleService) {
        this.cargaCombustibleService = cargaCombustibleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'CHOFER')")
    public ResponseEntity<List<CargaCombustibleResponse>> listar() {
        return ResponseEntity.ok(cargaCombustibleService.listar());
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'CHOFER')")
    public ResponseEntity<List<CargaCombustibleResponse>> listarPorVehiculo(@PathVariable Integer idVehiculo) {
        return ResponseEntity.ok(cargaCombustibleService.listarPorVehiculo(idVehiculo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'CHOFER')")
    public ResponseEntity<CargaCombustibleResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(cargaCombustibleService.obtener(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CHOFER')")
    public ResponseEntity<CargaCombustibleResponse> crear(
            @RequestPart("request") @Valid CargaCombustibleRequest request,
            @RequestPart(value = "comprobante", required = false) MultipartFile comprobante) {
        return ResponseEntity.ok(cargaCombustibleService.crear(request, comprobante));
    }

    @GetMapping("/{id}/comprobante")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'CHOFER')")
    public ResponseEntity<Resource> descargarComprobante(@PathVariable Integer id) {
        Resource resource = cargaCombustibleService.descargarComprobante(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprobante\"")
                .body(resource);
    }
}
