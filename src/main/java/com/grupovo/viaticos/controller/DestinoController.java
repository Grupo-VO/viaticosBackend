package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.DestinoRequest;
import com.grupovo.viaticos.dto.DestinoResponse;
import com.grupovo.viaticos.service.DestinoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/destinos")
public class DestinoController {

    private final DestinoService destinoService;

    public DestinoController(DestinoService destinoService) {
        this.destinoService = destinoService;
    }

    @GetMapping
    public ResponseEntity<List<DestinoResponse>> listar() {
        return ResponseEntity.ok(destinoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(destinoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DestinoResponse> crear(@Valid @RequestBody DestinoRequest request) {
        return ResponseEntity.ok(destinoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DestinoResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody DestinoRequest request) {
        return ResponseEntity.ok(destinoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id, @RequestBody Boolean activo) {
        destinoService.cambiarEstado(id, activo);
        return ResponseEntity.noContent().build();
    }
}
