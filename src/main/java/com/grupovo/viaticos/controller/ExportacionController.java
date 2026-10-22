package com.grupovo.viaticos.controller;

import com.grupovo.viaticos.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/exportaciones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITORIA')")
public class ExportacionController {

    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExportService exportService;

    public ExportacionController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/viajes.xlsx")
    public ResponseEntity<byte[]> viajesExcel(@RequestParam(required = false) LocalDate desde,
                                              @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.viajesExcel(desde, hasta), "viajes.xlsx", XLSX);
    }

    @GetMapping("/viajes.pdf")
    public ResponseEntity<byte[]> viajesPdf(@RequestParam(required = false) LocalDate desde,
                                            @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.viajesPdf(desde, hasta), "viajes.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping("/viaticos.xlsx")
    public ResponseEntity<byte[]> viaticosExcel(@RequestParam(required = false) LocalDate desde,
                                                @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.viaticosExcel(desde, hasta), "viaticos.xlsx", XLSX);
    }

    @GetMapping("/viaticos.pdf")
    public ResponseEntity<byte[]> viaticosPdf(@RequestParam(required = false) LocalDate desde,
                                              @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.viaticosPdf(desde, hasta), "viaticos.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping("/combustible.xlsx")
    public ResponseEntity<byte[]> combustibleExcel(@RequestParam(required = false) LocalDate desde,
                                                   @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.combustibleExcel(desde, hasta), "combustible.xlsx", XLSX);
    }

    @GetMapping("/combustible.pdf")
    public ResponseEntity<byte[]> combustiblePdf(@RequestParam(required = false) LocalDate desde,
                                                 @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.combustiblePdf(desde, hasta), "combustible.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping("/mantenimientos.xlsx")
    public ResponseEntity<byte[]> mantenimientosExcel(@RequestParam(required = false) LocalDate desde,
                                                      @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.mantenimientosExcel(desde, hasta), "mantenimientos.xlsx", XLSX);
    }

    @GetMapping("/mantenimientos.pdf")
    public ResponseEntity<byte[]> mantenimientosPdf(@RequestParam(required = false) LocalDate desde,
                                                    @RequestParam(required = false) LocalDate hasta) {
        return archivo(exportService.mantenimientosPdf(desde, hasta), "mantenimientos.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String nombre, String tipo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }
}
