package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Alerta;
import com.grupovo.viaticos.domain.UbicacionVehiculo;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.ViajeDestino;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import com.grupovo.viaticos.domain.en.AlertaTipo;
import com.grupovo.viaticos.dto.AlertaResponse;
import com.grupovo.viaticos.dto.MonitoreoViajeResponse;
import com.grupovo.viaticos.dto.UbicacionVehiculoResponse;
import com.grupovo.viaticos.dto.ViajeDestinoResponse;
import com.grupovo.viaticos.dto.ViajeResumenResponse;
import com.grupovo.viaticos.dto.IncidenteMapaResponse;
import com.grupovo.viaticos.repository.ReporteIncidenteRepository;
import com.grupovo.viaticos.repository.AlertaRepository;
import com.grupovo.viaticos.repository.UbicacionVehiculoRepository;
import com.grupovo.viaticos.repository.ViajeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.LocalDateTime;
import com.grupovo.viaticos.exception.NotFoundException;

@Service
public class MonitoreoService {

    private final ViajeRepository viajeRepository;
    private final UbicacionVehiculoRepository ubicacionRepository;
    private final AlertaRepository alertaRepository;
    private final ViajeService viajeService;
    private final ConfiguracionService configuracionService;
    private final ReporteIncidenteRepository reporteRepository;

    public MonitoreoService(ViajeRepository viajeRepository,
                            UbicacionVehiculoRepository ubicacionRepository,
                            AlertaRepository alertaRepository,
                            ViajeService viajeService,
                            ConfiguracionService configuracionService,
                            ReporteIncidenteRepository reporteRepository) {
        this.viajeRepository = viajeRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.alertaRepository = alertaRepository;
        this.viajeService = viajeService;
        this.configuracionService = configuracionService;
        this.reporteRepository = reporteRepository;
    }

    @Transactional
    public List<MonitoreoViajeResponse> viajesEnCurso() {
        List<Viaje> activos = viajeRepository.findByStatusInOrderByFechaSalidaAsc(List.of(ViajeStatus.EN_VIAJE));

        Map<Integer, UbicacionVehiculo> ultimasUbicaciones = new HashMap<>();
        for (Viaje viaje : activos) {
            UbicacionVehiculo ultima = ubicacionRepository.findFirstByViajeIdViajeOrderByFechaDesc(viaje.getIdViaje())
                    .orElse(null);
            if (ultima != null) {
                ultimasUbicaciones.put(viaje.getIdViaje(), ultima);
            }
            actualizarAlertaComunicacion(viaje, ultima);
        }

        Map<Integer, List<Alerta>> alertasPorViaje = new HashMap<>();
        if (!activos.isEmpty()) {
            List<Integer> ids = activos.stream().map(Viaje::getIdViaje).toList();
            alertaRepository.findByViajeIdViajeInAndActivaOrderByFechaDesc(ids, true)
                    .forEach(a -> alertasPorViaje
                            .computeIfAbsent(a.getViaje().getIdViaje(), k -> new ArrayList<>())
                            .add(a));
        }

        return activos.stream()
                .map(v -> toMonitoreo(v,
                        ultimasUbicaciones.get(v.getIdViaje()),
                        alertasPorViaje.getOrDefault(v.getIdViaje(), List.of())))
                .toList();
    }

    @Transactional
    public AlertaResponse atenderAlerta(Integer idAlerta) {
        Alerta alerta = alertaRepository.findById(idAlerta)
                .orElseThrow(() -> new NotFoundException("Alerta no encontrada con id " + idAlerta));
        alerta.setActiva(false);
        alerta = alertaRepository.save(alerta);
        return toAlertaResponse(alerta);
    }

    @Transactional(readOnly = true)
    public ViajeResumenResponse resumen(Integer id) {
        List<UbicacionVehiculo> ubicaciones = ubicacionRepository.findAllByViajeIdViajeOrderByFechaAsc(id);
        List<Alerta> alertas = alertaRepository.findByViajeIdViajeOrderByFechaDesc(id);
        List<IncidenteMapaResponse> incidentes = reporteRepository.findAllByViajeIdViajeOrderByFechaDesc(id).stream()
                .filter(r -> r.getLatitud() != null && r.getLongitud() != null)
                .map(r -> new IncidenteMapaResponse(r.getIdReporte(), r.getTipo().name(), r.getSeveridad(),
                        r.getDescripcion(), r.getLatitud(), r.getLongitud()))
                .toList();
        return new ViajeResumenResponse(
                viajeService.obtener(id),
                ubicaciones.stream().map(this::toUbicacionResponse).toList(),
                alertas.stream().map(this::toAlertaResponse).toList(),
                incidentes);
    }

    private MonitoreoViajeResponse toMonitoreo(Viaje viaje, UbicacionVehiculo ultima, List<Alerta> alertas) {
        List<ViajeDestino> destinos = viaje.getViajeDestinos().stream()
                .sorted(Comparator.comparing(ViajeDestino::getOrden))
                .toList();
        int total = (int) destinos.stream().filter(d -> d.getOrden() != 0).count();
        int visitados = (int) destinos.stream()
                .filter(d -> d.getOrden() != 0 && Boolean.TRUE.equals(d.getVisitado()))
                .count();

        return new MonitoreoViajeResponse(
                viaje.getIdViaje(),
                viaje.getVehiculo().getIdVehiculo(),
                viaje.getVehiculo().getPlaca(),
                viaje.getVehiculo().getMarca() + " " + viaje.getVehiculo().getModelo(),
                viaje.getChofer().getIdPersona(),
                viaje.getChofer().getNombre() + " " + viaje.getChofer().getApellidoPaterno(),
                viaje.getViajeDestinos().stream()
                        .filter(d -> d.getOrden() == 0).findFirst()
                        .map(ViajeDestino::getNombre).orElse(null),
                viaje.getViajeDestinos().stream()
                        .filter(d -> d.getOrden() != 0 && !Boolean.TRUE.equals(d.getVisitado()))
                        .min(Comparator.comparing(ViajeDestino::getOrden))
                        .map(ViajeDestino::getNombre).orElse(null),
                ultima != null ? ultima.getLatitud() : null,
                ultima != null ? ultima.getLongitud() : null,
                ultima != null ? ultima.getVelocidadKmh() : null,
                ultima != null ? ultima.getFecha() : null,
                total,
                visitados,
                destinos.stream().map(this::toDestinoResponse).toList(),
                alertas.stream().map(this::toAlertaResponse).toList(),
                viaje.getVehiculo().getNivelCombustible(),
                viaje.getVehiculo().getCapacidadTanqueLitros(),
                viaje.getFechaSalida(),
                alertas.stream().anyMatch(a -> a.getTipo() == AlertaTipo.COMUNICACION_PERDIDA));
    }

    private void actualizarAlertaComunicacion(Viaje viaje, UbicacionVehiculo ultima) {
        LocalDateTime referencia = ultima != null ? ultima.getFecha() : viaje.getFechaInicio();
        if (referencia == null) {
            return;
        }
        long segundosSinUbicacion = Math.max(0, Duration.between(referencia, LocalDateTime.now()).getSeconds());
        long limite = Math.max(1, Math.round(configuracionService.getDouble(
                ConfiguracionService.TIEMPO_SIN_COMUNICACION_SEGUNDOS, 90.0)));
        if (segundosSinUbicacion >= limite) {
            crearAlertaSiNoActiva(viaje, "Sin ubicacion recibida durante " + segundosSinUbicacion + " segundos");
        } else {
            resolverAlertas(viaje, AlertaTipo.COMUNICACION_PERDIDA);
        }
    }

    private void crearAlertaSiNoActiva(Viaje viaje, String mensaje) {
        if (alertaRepository.existsByViajeIdViajeAndTipoAndActiva(
                viaje.getIdViaje(), AlertaTipo.COMUNICACION_PERDIDA, true)) {
            return;
        }
        alertaRepository.save(Alerta.builder()
                .viaje(viaje)
                .vehiculo(viaje.getVehiculo())
                .tipo(AlertaTipo.COMUNICACION_PERDIDA)
                .mensaje(mensaje)
                .fecha(LocalDateTime.now())
                .activa(true)
                .build());
    }

    private void resolverAlertas(Viaje viaje, AlertaTipo tipo) {
        List<Alerta> activas = alertaRepository.findByViajeIdViajeAndTipoAndActiva(viaje.getIdViaje(), tipo, true);
        activas.forEach(a -> a.setActiva(false));
        if (!activas.isEmpty()) {
            alertaRepository.saveAll(activas);
        }
    }

    private ViajeDestinoResponse toDestinoResponse(ViajeDestino d) {
        return new ViajeDestinoResponse(
                d.getIdViajeDestino(),
                d.getNombre(),
                d.getDireccion(),
                d.getLatitud(),
                d.getLongitud(),
                d.getOrden(),
                d.getVisitado(),
                d.getFechaLlegada() != null ? d.getFechaLlegada().toString() : null);
    }

    private UbicacionVehiculoResponse toUbicacionResponse(UbicacionVehiculo u) {
        return new UbicacionVehiculoResponse(
                u.getIdUbicacion(),
                u.getViaje().getIdViaje(),
                u.getVehiculo().getIdVehiculo(),
                u.getLatitud(),
                u.getLongitud(),
                u.getVelocidadKmh(),
                u.getFecha());
    }

    private AlertaResponse toAlertaResponse(Alerta a) {
        return new AlertaResponse(
                a.getIdAlerta(),
                a.getViaje().getIdViaje(),
                a.getVehiculo().getIdVehiculo(),
                a.getTipo(),
                a.getMensaje(),
                a.getFecha(),
                a.getActiva());
    }
}
