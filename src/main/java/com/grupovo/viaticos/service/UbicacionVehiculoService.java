package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Alerta;
import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.UbicacionVehiculo;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.ViajeDestino;
import com.grupovo.viaticos.domain.en.AlertaTipo;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.domain.en.TipoTrayecto;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import com.grupovo.viaticos.dto.UbicacionVehiculoRequest;
import com.grupovo.viaticos.dto.UbicacionVehiculoResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.repository.AlertaRepository;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.UbicacionVehiculoRepository;
import com.grupovo.viaticos.util.PolylineUtils;
import com.grupovo.viaticos.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class UbicacionVehiculoService {

    private final UbicacionVehiculoRepository ubicacionRepository;
    private final AlertaRepository alertaRepository;
    private final PersonaRepository personaRepository;
    private final ViajeService viajeService;
    private final ConfiguracionService configuracionService;

    public UbicacionVehiculoService(UbicacionVehiculoRepository ubicacionRepository,
                                    AlertaRepository alertaRepository,
                                    PersonaRepository personaRepository,
                                    ViajeService viajeService,
                                    ConfiguracionService configuracionService) {
        this.ubicacionRepository = ubicacionRepository;
        this.alertaRepository = alertaRepository;
        this.personaRepository = personaRepository;
        this.viajeService = viajeService;
        this.configuracionService = configuracionService;
    }

    @Transactional
    public UbicacionVehiculoResponse registrar(UbicacionVehiculoRequest request) {
        Viaje viaje = viajeService.getViaje(request.idViaje());
        if (viaje.getStatus() != ViajeStatus.EN_VIAJE) {
            throw new InvalidEntryException("El viaje debe estar EN VIAJE para registrar ubicaciones");
        }
        validarAcceso(viaje);

        UbicacionVehiculo ubicacion = UbicacionVehiculo.builder()
                .viaje(viaje)
                .vehiculo(viaje.getVehiculo())
                .latitud(request.latitud())
                .longitud(request.longitud())
                .velocidadKmh(request.velocidadKmh() == null ? 0.0 : request.velocidadKmh())
                .fecha(request.fecha() != null ? request.fecha() : LocalDateTime.now())
                .build();
        ubicacion = ubicacionRepository.save(ubicacion);

        verificarGeocerca(viaje, ubicacion.getLatitud(), ubicacion.getLongitud());
        return toResponse(ubicacion);
    }

    @Transactional(readOnly = true)
    public List<UbicacionVehiculoResponse> historial(Integer idViaje) {
        Viaje viaje = viajeService.getViaje(idViaje);
        validarAcceso(viaje);
        return ubicacionRepository.findAllByViajeIdViajeOrderByFechaAsc(idViaje).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Alerta> alertasDelViaje(Integer idViaje) {
        return alertaRepository.findByViajeIdViajeOrderByFechaDesc(idViaje);
    }

    @Transactional(readOnly = true)
    public List<Alerta> alertasActivas(Integer idViaje) {
        return alertaRepository.findByViajeIdViajeAndActivaOrderByFechaDesc(idViaje, true);
    }

    private void verificarGeocerca(Viaje viaje, double latitud, double longitud) {
        verificarLlegadaDestino(viaje, latitud, longitud);
        verificarDesviacion(viaje, latitud, longitud);
        verificarGasolinaBaja(viaje);
    }

    private void verificarLlegadaDestino(Viaje viaje, double latitud, double longitud) {
        List<ViajeDestino> destinos = viaje.getViajeDestinos().stream()
                .sorted(Comparator.comparing(ViajeDestino::getOrden))
                .toList();
        ViajeDestino siguiente = destinos.stream()
                .filter(d -> d.getOrden() != 0 && !Boolean.TRUE.equals(d.getVisitado()))
                .findFirst()
                .orElse(null);
        if (siguiente == null) {
            return;
        }
        double radioLlegada = configuracionService.getDouble(ConfiguracionService.RADIO_LLEGADA_METROS, 100.0);
        double distancia = PolylineUtils.distanciaMetros(
                latitud, longitud, siguiente.getLatitud(), siguiente.getLongitud());
        if (distancia <= radioLlegada) {
            siguiente.setVisitado(true);
            siguiente.setFechaLlegada(LocalDateTime.now());
            crearAlertaSiNoActiva(viaje, AlertaTipo.LLEGADA_DESTINO,
                    "Llegada registrada al destino " + siguiente.getNombre() + " (orden " + siguiente.getOrden() + ")");
        }
    }

    private void verificarDesviacion(Viaje viaje, double latitud, double longitud) {
        String polyline = viaje.getPolylineCasetas();
        if ((polyline == null || polyline.isEmpty()) || viaje.getTipoTrayecto() == TipoTrayecto.LIBRE) {
            polyline = viaje.getPolylineLibre();
        }
        if (polyline == null || polyline.isEmpty()) {
            return;
        }
        double umbral = configuracionService.getDouble(ConfiguracionService.DISTANCIA_ALERTA_METROS, 150.0);
        double distancia = distanciaAPolyline(polyline, latitud, longitud);
        if (distancia > umbral) {
            crearAlertaSiNoActiva(viaje, AlertaTipo.DESVIACION,
                    "El vehículo se encuentra a " + Math.round(distancia) + " m de la ruta planificada");
        } else {
            resolverAlertas(viaje, AlertaTipo.DESVIACION);
        }
    }

    private void verificarGasolinaBaja(Viaje viaje) {
        double umbralPct = configuracionService.getDouble(ConfiguracionService.UMBRAL_GASOLINA, 20.0);
        double umbralLitros = viaje.getVehiculo().getCapacidadTanqueLitros() * umbralPct / 100.0;
        if (viaje.getVehiculo().getNivelCombustible() < umbralLitros) {
            crearAlertaSiNoActiva(viaje, AlertaTipo.GASOLINA_BAJA,
                    "Nivel de combustible bajo: " + Math.round(viaje.getVehiculo().getNivelCombustible()) + " L");
        }
    }

    private double distanciaAPolyline(String polyline, double latitud, double longitud) {
        List<double[]> puntos = PolylineUtils.decode(polyline);
        if (puntos.size() < 2) {
            return Double.MAX_VALUE;
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < puntos.size() - 1; i++) {
            double d = PolylineUtils.distanciaPuntoASegmento(
                    latitud, longitud,
                    puntos.get(i)[0], puntos.get(i)[1],
                    puntos.get(i + 1)[0], puntos.get(i + 1)[1]);
            min = Math.min(min, d);
        }
        return min;
    }

    private void crearAlertaSiNoActiva(Viaje viaje, AlertaTipo tipo, String mensaje) {
        if (alertaRepository.existsByViajeIdViajeAndTipoAndActiva(viaje.getIdViaje(), tipo, true)) {
            return;
        }
        alertaRepository.save(Alerta.builder()
                .viaje(viaje)
                .vehiculo(viaje.getVehiculo())
                .tipo(tipo)
                .mensaje(mensaje)
                .fecha(LocalDateTime.now())
                .activa(true)
                .build());
    }

    private void resolverAlertas(Viaje viaje, AlertaTipo tipo) {
        List<Alerta> activas = alertaRepository.findByViajeIdViajeAndTipoAndActiva(viaje.getIdViaje(), tipo, true);
        activas.forEach(a -> a.setActiva(false));
        alertaRepository.saveAll(activas);
    }

    private void validarAcceso(Viaje viaje) {
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return;
        }
        Persona current = personaRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (current != null && current.getRole() == Role.CHOFER
                && !current.getIdPersona().equals(viaje.getChofer().getIdPersona())) {
            throw new InvalidEntryException("Este viaje no está asignado a tu usuario");
        }
    }

    private UbicacionVehiculoResponse toResponse(UbicacionVehiculo u) {
        return new UbicacionVehiculoResponse(
                u.getIdUbicacion(),
                u.getViaje().getIdViaje(),
                u.getVehiculo().getIdVehiculo(),
                u.getLatitud(),
                u.getLongitud(),
                u.getVelocidadKmh(),
                u.getFecha());
    }
}
