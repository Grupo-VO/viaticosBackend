package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Alerta;
import com.grupovo.viaticos.domain.UbicacionVehiculo;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.ViajeDestino;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import com.grupovo.viaticos.dto.AlertaResponse;
import com.grupovo.viaticos.dto.MonitoreoViajeResponse;
import com.grupovo.viaticos.dto.UbicacionVehiculoResponse;
import com.grupovo.viaticos.dto.ViajeDestinoResponse;
import com.grupovo.viaticos.dto.ViajeResumenResponse;
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

@Service
public class MonitoreoService {

    private final ViajeRepository viajeRepository;
    private final UbicacionVehiculoRepository ubicacionRepository;
    private final AlertaRepository alertaRepository;
    private final ViajeService viajeService;

    public MonitoreoService(ViajeRepository viajeRepository,
                            UbicacionVehiculoRepository ubicacionRepository,
                            AlertaRepository alertaRepository,
                            ViajeService viajeService) {
        this.viajeRepository = viajeRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.alertaRepository = alertaRepository;
        this.viajeService = viajeService;
    }

    @Transactional(readOnly = true)
    public List<MonitoreoViajeResponse> viajesEnCurso() {
        List<Viaje> activos = viajeRepository.findByStatusInOrderByFechaSalidaAsc(List.of(ViajeStatus.EN_VIAJE));

        Map<Integer, UbicacionVehiculo> ultimasUbicaciones = new HashMap<>();
        for (Viaje viaje : activos) {
            ubicacionRepository.findFirstByViajeIdViajeOrderByFechaDesc(viaje.getIdViaje())
                    .ifPresent(u -> ultimasUbicaciones.put(viaje.getIdViaje(), u));
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

    @Transactional(readOnly = true)
    public ViajeResumenResponse resumen(Integer id) {
        List<UbicacionVehiculo> ubicaciones = ubicacionRepository.findAllByViajeIdViajeOrderByFechaAsc(id);
        List<Alerta> alertas = alertaRepository.findByViajeIdViajeOrderByFechaDesc(id);
        return new ViajeResumenResponse(
                viajeService.obtener(id),
                ubicaciones.stream().map(this::toUbicacionResponse).toList(),
                alertas.stream().map(this::toAlertaResponse).toList());
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
                viaje.getFechaSalida());
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
