package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.ReporteIncidente;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.dto.ReporteIncidenteRequest;
import com.grupovo.viaticos.dto.ReporteIncidenteResponse;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.ReporteIncidenteRepository;
import com.grupovo.viaticos.util.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ReporteIncidenteService {

    private final ReporteIncidenteRepository reporteRepository;
    private final PersonaRepository personaRepository;
    private final ViajeService viajeService;
    private final StorageService storageService;

    public ReporteIncidenteService(ReporteIncidenteRepository reporteRepository,
                                   PersonaRepository personaRepository,
                                   ViajeService viajeService,
                                   StorageService storageService) {
        this.reporteRepository = reporteRepository;
        this.personaRepository = personaRepository;
        this.viajeService = viajeService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<ReporteIncidenteResponse> listar() {
        List<ReporteIncidente> reportes;
        Persona current = personaActual();
        if (current != null && current.getRole() == Role.CHOFER) {
            reportes = reporteRepository.findAllByCreadoPorIdPersonaOrderByFechaDesc(current.getIdPersona());
        } else {
            reportes = reporteRepository.findAllByOrderByFechaDesc();
        }
        return reportes.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReporteIncidenteResponse obtener(Integer id) {
        ReporteIncidente reporte = getReporte(id);
        Persona current = personaActual();
        if (current != null && current.getRole() == Role.CHOFER
                && !reporte.getCreadoPor().getIdPersona().equals(current.getIdPersona())) {
            throw new NotFoundException("Reporte no encontrado con id " + id);
        }
        return toResponse(reporte);
    }

    @Transactional
    public ReporteIncidenteResponse crear(ReporteIncidenteRequest request, MultipartFile evidencia) {
        Persona creador = personaActual();
        if (creador == null || !creador.getActiva()) {
            throw new NotFoundException("Usuario no encontrado");
        }
        Viaje viaje = request.idViaje() == null ? null : viajeService.getViaje(request.idViaje());

        String evidenciaRuta = storageService.guardar(evidencia, "reportes");

        ReporteIncidente reporte = ReporteIncidente.builder()
                .viaje(viaje)
                .tipo(request.tipo())
                .descripcion(request.descripcion().trim())
                .fecha(request.fecha())
                .creadoPor(creador)
                .evidenciaRuta(evidenciaRuta)
                .build();
        return toResponse(reporteRepository.save(reporte));
    }

    @Transactional
    public void eliminar(Integer id) {
        ReporteIncidente reporte = getReporte(id);
        Persona current = personaActual();
        if (current != null && current.getRole() == Role.CHOFER
                && !reporte.getCreadoPor().getIdPersona().equals(current.getIdPersona())) {
            throw new NotFoundException("Reporte no encontrado con id " + id);
        }
        reporteRepository.delete(reporte);
    }

    @Transactional(readOnly = true)
    public Resource descargarEvidencia(Integer id) {
        return storageService.cargar(getReporte(id).getEvidenciaRuta());
    }

    private Persona personaActual() {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return null;
        }
        return personaRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    public ReporteIncidente getReporte(Integer id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reporte no encontrado con id " + id));
    }

    private ReporteIncidenteResponse toResponse(ReporteIncidente r) {
        Viaje viaje = r.getViaje();
        Persona creador = r.getCreadoPor();
        return new ReporteIncidenteResponse(
                r.getIdReporte(),
                viaje == null ? null : viaje.getIdViaje(),
                viaje == null ? null : viaje.getRuta().getNombre(),
                r.getTipo(),
                r.getDescripcion(),
                r.getFecha(),
                creador.getIdPersona(),
                creador.getNombre() + " " + creador.getApellidoPaterno(),
                r.getEvidenciaRuta(),
                r.getFechaCreacion()
        );
    }
}
