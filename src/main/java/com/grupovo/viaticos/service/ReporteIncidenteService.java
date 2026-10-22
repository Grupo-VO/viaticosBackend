package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.ReporteIncidente;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.domain.en.ReporteIncidenteEstado;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import com.grupovo.viaticos.domain.en.AlertaTipo;
import com.grupovo.viaticos.dto.ReporteIncidenteRequest;
import com.grupovo.viaticos.dto.ReporteIncidenteResponse;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.ReporteIncidenteRepository;
import com.grupovo.viaticos.repository.AlertaRepository;
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
    private final AlertaRepository alertaRepository;

    public ReporteIncidenteService(ReporteIncidenteRepository reporteRepository,
                                   PersonaRepository personaRepository,
                                   ViajeService viajeService,
                                   StorageService storageService,
                                   AlertaRepository alertaRepository) {
        this.reporteRepository = reporteRepository;
        this.personaRepository = personaRepository;
        this.viajeService = viajeService;
        this.storageService = storageService;
        this.alertaRepository = alertaRepository;
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
        if (viaje != null && creador.getRole() == Role.CHOFER) {
            if (!viaje.getChofer().getIdPersona().equals(creador.getIdPersona())) {
                throw new InvalidEntryException("El viaje no está asignado a tu usuario");
            }
            if (viaje.getStatus() != ViajeStatus.EN_VIAJE) {
                throw new InvalidEntryException("Sólo puedes reportar incidentes durante un viaje activo");
            }
        }
        validarCoordenadas(request.latitud(), request.longitud());

        String evidenciaRuta = storageService.guardar(evidencia, "reportes");

        ReporteIncidente reporte = ReporteIncidente.builder()
                .viaje(viaje)
                .tipo(request.tipo())
                .severidad(request.severidad())
                .estado(ReporteIncidenteEstado.NUEVO)
                .descripcion(request.descripcion().trim())
                .fecha(request.fecha())
                .creadoPor(creador)
                .evidenciaRuta(evidenciaRuta)
                .latitud(request.latitud())
                .longitud(request.longitud())
                .build();
        reporte = reporteRepository.save(reporte);
        if (viaje != null && request.severidad().name().equals("CRITICA")) {
            alertaRepository.save(com.grupovo.viaticos.domain.Alerta.builder()
                    .viaje(viaje)
                    .vehiculo(viaje.getVehiculo())
                    .tipo(AlertaTipo.INCIDENTE_CRITICO)
                    .mensaje("Incidente crítico reportado: " + reporte.getDescripcion())
                    .fecha(reporte.getFecha())
                    .activa(true)
                    .build());
        }
        return toResponse(reporte);
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

    @Transactional
    public ReporteIncidenteResponse cambiarEstado(Integer id, ReporteIncidenteEstado estado) {
        if (estado == null) {
            throw new InvalidEntryException("El estado es obligatorio");
        }
        ReporteIncidente reporte = getReporte(id);
        reporte.setEstado(estado);
        return toResponse(reporteRepository.save(reporte));
    }

    @Transactional(readOnly = true)
    public Resource descargarEvidencia(Integer id) {
        ReporteIncidente reporte = getReporte(id);
        Persona current = personaActual();
        if (current != null && current.getRole() == Role.CHOFER
                && !reporte.getCreadoPor().getIdPersona().equals(current.getIdPersona())) {
            throw new NotFoundException("Archivo no encontrado");
        }
        return storageService.cargar(reporte.getEvidenciaRuta());
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
                viaje == null ? null : viajeService.nombreRuta(viaje),
                r.getTipo(),
                r.getSeveridad(),
                r.getEstado() == null ? ReporteIncidenteEstado.NUEVO : r.getEstado(),
                r.getDescripcion(),
                r.getFecha(),
                creador.getIdPersona(),
                creador.getNombre() + " " + creador.getApellidoPaterno(),
                r.getEvidenciaRuta(),
                r.getLatitud(),
                r.getLongitud(),
                r.getFechaCreacion()
        );
    }

    private void validarCoordenadas(Double latitud, Double longitud) {
        if ((latitud == null) != (longitud == null)
                || (latitud != null && (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180))) {
            throw new InvalidEntryException("Las coordenadas del incidente no son válidas");
        }
    }
}
