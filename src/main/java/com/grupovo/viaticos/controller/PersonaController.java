package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.dto.PersonaRequest;
import com.grupovo.viaticos.dto.PersonaResponse;
import com.grupovo.viaticos.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/personas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping
    public ResponseEntity<List<PersonaResponse>> listar(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(personaService.listar(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(personaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PersonaResponse> crear(@Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonaResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody PersonaRequest request) {
        return ResponseEntity.ok(personaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PersonaResponse> cambiarEstado(@PathVariable Integer id, @RequestBody Boolean activa) {
        return ResponseEntity.ok(personaService.cambiarEstado(id, activa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
