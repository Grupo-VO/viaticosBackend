package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.ConfiguracionRequest;
import com.grupovo.viaticos.dto.ConfiguracionResponse;
import com.grupovo.viaticos.service.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/configuraciones")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public ResponseEntity<List<ConfiguracionResponse>> listar() {
        return ResponseEntity.ok(configuracionService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfiguracionResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(configuracionService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConfiguracionResponse> crear(@Valid @RequestBody ConfiguracionRequest request) {
        return ResponseEntity.ok(configuracionService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConfiguracionResponse> actualizar(@PathVariable Integer id,
                                                            @Valid @RequestBody ConfiguracionRequest request) {
        return ResponseEntity.ok(configuracionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        configuracionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
