package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.ViajeRequest;
import com.grupovo.viaticos.dto.ViajeResponse;
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

    public ViajeController(ViajeService viajeService) {
        this.viajeService = viajeService;
    }

    @GetMapping
    public ResponseEntity<List<ViajeResponse>> listar() {
        return ResponseEntity.ok(viajeService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViajeResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(viajeService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO_AUTORIZADO')")
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
}
