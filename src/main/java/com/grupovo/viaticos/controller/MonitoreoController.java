package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.dto.MonitoreoViajeResponse;
import com.grupovo.viaticos.service.MonitoreoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitoreo")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    public MonitoreoController(MonitoreoService monitoreoService) {
        this.monitoreoService = monitoreoService;
    }

    @GetMapping("/viajes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA', 'USUARIO_AUTORIZADO')")
    public ResponseEntity<List<MonitoreoViajeResponse>> viajesEnCurso() {
        return ResponseEntity.ok(monitoreoService.viajesEnCurso());
    }
}
