package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Caseta;
import com.grupovo.viaticos.domain.Destino;
import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.Ruta;
import com.grupovo.viaticos.domain.Vehiculo;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.ViajeCaseta;
import com.grupovo.viaticos.domain.ViajeDestino;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.domain.en.TipoTrayecto;
import com.grupovo.viaticos.domain.en.VehiculoStatus;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import com.grupovo.viaticos.dto.ViajeCasetaResponse;
import com.grupovo.viaticos.dto.ViajeDestinoResponse;
import com.grupovo.viaticos.dto.ViajeRequest;
import com.grupovo.viaticos.dto.ViajeResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.gmaps.GoogleMapsService;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.VehiculoRepository;
import com.grupovo.viaticos.repository.ViajeRepository;
import com.grupovo.viaticos.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ViajeService {

    private final ViajeRepository viajeRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PersonaRepository personaRepository;
    private final RutaService rutaService;
    private final ConfiguracionService configuracionService;
    private final DestinoService destinoService;
    private final GoogleMapsService googleMapsService;

    public ViajeService(ViajeRepository viajeRepository,
                        VehiculoRepository vehiculoRepository,
                        PersonaRepository personaRepository,
                        RutaService rutaService,
                        ConfiguracionService configuracionService,
                        DestinoService destinoService,
                        GoogleMapsService googleMapsService) {
        this.viajeRepository = viajeRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.personaRepository = personaRepository;
        this.rutaService = rutaService;
        this.configuracionService = configuracionService;
        this.destinoService = destinoService;
        this.googleMapsService = googleMapsService;
    }

    @Transactional(readOnly = true)
    public List<ViajeResponse> listar() {
        String username = SecurityUtils.currentUsername();
        Persona current = username == null ? null
                : personaRepository.findByUsernameIgnoreCase(username).orElse(null);

        List<Viaje> viajes;
        if (current != null && current.getRole() == Role.CHOFER) {
            viajes = viajeRepository.findAllByChoferIdPersonaOrderByFechaSalidaDesc(current.getIdPersona());
        } else {
            viajes = viajeRepository.findAllByOrderByFechaSalidaDesc();
        }
        return viajes.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ViajeResponse obtener(Integer id) {
        Viaje viaje = getViaje(id);
        validarAccesoChofer(viaje);
        return toResponse(viaje);
    }

    @Transactional
    public ViajeResponse crear(ViajeRequest request) {
        Vehiculo vehiculo = getVehiculoDisponible(request.idVehiculo());
        Persona chofer = getChofer(request.idChofer());
        validarFechas(request.fechaSalida(), request.fechaRegreso());
        validarModoRuta(request);
        validarDestinosViaje(request);

        boolean excesoCapacidad = request.numeroPasajeros() > vehiculo.getLimitePasajeros();

        Viaje viaje = Viaje.builder()
                .vehiculo(vehiculo)
                .chofer(chofer)
                .numeroPasajeros(request.numeroPasajeros())
                .fechaSalida(request.fechaSalida())
                .fechaRegreso(request.fechaRegreso())
                .tipoTrayecto(request.tipoTrayecto())
                .status(ViajeStatus.PROXIMO)
                .excesoCapacidad(excesoCapacidad)
                .costoComida(request.costoComida())
                .costoHospedaje(request.costoHospedaje())
                .build();

        if (request.idRuta() != null) {
            usarRutaGuardada(viaje, request.idRuta());
        } else {
            List<Destino> destinos = resolverDestinos(request);
            GoogleMapsService.RouteData routeData =
                    googleMapsService.calcularRutaMultiDestino(destinos, Boolean.TRUE.equals(request.optimizar()));
            viaje.setDistanciaKm(routeData.distanciaKm());
            viaje.setDuracionMinutos(routeData.duracionMinutos());
            viaje.setPolylineCasetas(routeData.polylineCasetas());
            viaje.setPolylineLibre(routeData.polylineLibre());
            viaje.setPolylineCasetasIda(routeData.polylineCasetasIda());
            viaje.setPolylineCasetasRegreso(routeData.polylineCasetasRegreso());
            viaje.setPolylineLibreIda(routeData.polylineLibreIda());
            viaje.setPolylineLibreRegreso(routeData.polylineLibreRegreso());
            poblarDestinos(viaje, destinos);
            poblarCasetas(viaje, routeData.casetas());
        }

        validarSinTraslapes(vehiculo.getIdVehiculo(), chofer.getIdPersona(),
                request.fechaSalida(), request.fechaRegreso(), null);

        if (vehiculo.getStatus() != VehiculoStatus.EN_SERVICIO) {
            vehiculo.setStatus(VehiculoStatus.EN_SERVICIO);
            vehiculoRepository.save(vehiculo);
        }

        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse actualizar(Integer id, ViajeRequest request) {
        Viaje viaje = getViaje(id);
        if (viaje.getStatus() != ViajeStatus.PROXIMO) {
            throw new InvalidEntryException("Solo se pueden editar viajes en estado PRÓXIMO");
        }

        Vehiculo vehiculoAnterior = viaje.getVehiculo();
        Vehiculo vehiculo = getVehiculoDisponible(request.idVehiculo());
        Persona chofer = getChofer(request.idChofer());
        validarFechas(request.fechaSalida(), request.fechaRegreso());
        validarModoRuta(request);
        validarDestinosViaje(request);

        viaje.setVehiculo(vehiculo);
        viaje.setChofer(chofer);
        viaje.setNumeroPasajeros(request.numeroPasajeros());
        viaje.setFechaSalida(request.fechaSalida());
        viaje.setFechaRegreso(request.fechaRegreso());
        viaje.setTipoTrayecto(request.tipoTrayecto());
        viaje.setExcesoCapacidad(request.numeroPasajeros() > vehiculo.getLimitePasajeros());
        viaje.setCostoComida(request.costoComida());
        viaje.setCostoHospedaje(request.costoHospedaje());

        viaje.getViajeDestinos().clear();
        viaje.getViajeCasetas().clear();

        if (request.idRuta() != null) {
            usarRutaGuardada(viaje, request.idRuta());
        } else {
            viaje.setRuta(null);
            List<Destino> destinos = resolverDestinos(request);
            GoogleMapsService.RouteData routeData =
                    googleMapsService.calcularRutaMultiDestino(destinos, Boolean.TRUE.equals(request.optimizar()));
            viaje.setDistanciaKm(routeData.distanciaKm());
            viaje.setDuracionMinutos(routeData.duracionMinutos());
            viaje.setPolylineCasetas(routeData.polylineCasetas());
            viaje.setPolylineLibre(routeData.polylineLibre());
            viaje.setPolylineCasetasIda(routeData.polylineCasetasIda());
            viaje.setPolylineCasetasRegreso(routeData.polylineCasetasRegreso());
            viaje.setPolylineLibreIda(routeData.polylineLibreIda());
            viaje.setPolylineLibreRegreso(routeData.polylineLibreRegreso());
            poblarDestinos(viaje, destinos);
            poblarCasetas(viaje, routeData.casetas());
        }

        validarSinTraslapes(vehiculo.getIdVehiculo(), chofer.getIdPersona(),
                request.fechaSalida(), request.fechaRegreso(), id);

        if (!vehiculoAnterior.getIdVehiculo().equals(vehiculo.getIdVehiculo())
                && vehiculoAnterior.getStatus() != VehiculoStatus.MANTENIMIENTO
                && vehiculoAnterior.getStatus() != VehiculoStatus.FUERA_SERVICIO) {
            vehiculoAnterior.setStatus(disponibilidadVehiculo(vehiculoAnterior));
            vehiculoRepository.save(vehiculoAnterior);
        }
        if (vehiculo.getStatus() != VehiculoStatus.EN_SERVICIO) {
            vehiculo.setStatus(VehiculoStatus.EN_SERVICIO);
            vehiculoRepository.save(vehiculo);
        }

        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse cancelar(Integer id) {
        Viaje viaje = getViaje(id);
        if (viaje.getStatus() != ViajeStatus.PROXIMO) {
            throw new InvalidEntryException("Solo se pueden cancelar viajes en estado PRÓXIMO");
        }
        viaje.setStatus(ViajeStatus.CANCELADO);
        liberarVehiculo(viaje.getVehiculo());
        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse completar(Integer id) {
        Viaje viaje = getViaje(id);
        if (viaje.getStatus() != ViajeStatus.EN_VIAJE) {
            throw new InvalidEntryException("Solo se pueden completar viajes en estado EN VIAJE");
        }
        viaje.setStatus(ViajeStatus.COMPLETADO);

        Vehiculo vehiculo = viaje.getVehiculo();
        double litrosConsumidos = litrosConsumidos(viaje, vehiculo);
        vehiculo.setNivelCombustible(Math.max(0.0, vehiculo.getNivelCombustible() - litrosConsumidos));
        vehiculo.setKilometraje(vehiculo.getKilometraje() + distanciaKm(viaje));
        vehiculoRepository.save(vehiculo);

        liberarVehiculo(vehiculo);
        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional
    public ViajeResponse iniciar(Integer id) {
        Viaje viaje = getViaje(id);
        if (viaje.getStatus() != ViajeStatus.PROXIMO) {
            throw new InvalidEntryException("Solo se pueden iniciar viajes en estado PRÓXIMO");
        }
        Persona current = currentUser();
        if (current != null && current.getRole() == Role.CHOFER
                && !current.getIdPersona().equals(viaje.getChofer().getIdPersona())) {
            throw new InvalidEntryException("Este viaje no está asignado a tu usuario");
        }
        viaje.setStatus(ViajeStatus.EN_VIAJE);
        viaje.setFechaInicio(LocalDateTime.now());
        return toResponse(viajeRepository.save(viaje));
    }

    @Transactional(readOnly = true)
    public ViajeResponse viajeActivo() {
        Persona current = currentUser();
        if (current == null || current.getRole() != Role.CHOFER) {
            throw new InvalidEntryException("Solo el chofer puede consultar su viaje activo");
        }
        List<Viaje> activos = viajeRepository.findByChoferIdPersonaAndStatusOrderByFechaSalidaAsc(
                current.getIdPersona(), ViajeStatus.EN_VIAJE);
        if (activos.isEmpty()) {
            throw new NotFoundException("No tienes un viaje activo");
        }
        return toResponse(activos.get(0));
    }

    private Persona currentUser() {
        String username = SecurityUtils.currentUsername();
        return username == null ? null
                : personaRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    private void validarAccesoChofer(Viaje viaje) {
        Persona current = currentUser();
        if (current != null && current.getRole() == Role.CHOFER
                && !current.getIdPersona().equals(viaje.getChofer().getIdPersona())) {
            throw new NotFoundException("Viaje no encontrado con id " + viaje.getIdViaje());
        }
    }

    private void validarDestinosViaje(ViajeRequest request) {
        if (request.idRuta() != null) {
            return;
        }
        if (request.idOrigen() == null) {
            throw new InvalidEntryException("Debes indicar el origen de la ruta");
        }
        if (request.idDestinos() == null || request.idDestinos().isEmpty()) {
            throw new InvalidEntryException("Debes indicar al menos un destino");
        }
    }

    private void validarModoRuta(ViajeRequest request) {
        boolean rutaPredefinida = request.idRuta() != null;
        boolean tieneOrigen = request.idOrigen() != null;
        boolean tieneDestinos = request.idDestinos() != null && !request.idDestinos().isEmpty();

        if (rutaPredefinida && (tieneOrigen || tieneDestinos)) {
            throw new InvalidEntryException("Una ruta predefinida no puede combinarse con destinos especiales");
        }
        if (!rutaPredefinida && (!tieneOrigen || !tieneDestinos)) {
            throw new InvalidEntryException("Una ruta especial requiere origen y al menos un destino");
        }
    }

    private void usarRutaGuardada(Viaje viaje, Integer idRuta) {
        Ruta ruta = rutaService.getRuta(idRuta);
        if (!Boolean.TRUE.equals(ruta.getActivo())) {
            throw new InvalidEntryException("La ruta está inactiva");
        }
        viaje.setRuta(ruta);
        viaje.setTipoTrayecto(Boolean.TRUE.equals(ruta.getUsaCasetas())
                ? TipoTrayecto.CASETAS : TipoTrayecto.LIBRE);
        viaje.setCostoComida(ruta.getCostoComida());
        viaje.setCostoHospedaje(ruta.getCostoHospedaje());
        viaje.setNombreRutaSnapshot(ruta.getNombre());
        viaje.setDistanciaKm(ruta.getDistanciaKm());
        viaje.setDuracionMinutos(ruta.getDuracionMinutos());
        viaje.setPolylineCasetas(ruta.getPolylineCasetas());
        viaje.setPolylineLibre(ruta.getPolylineLibre());
        viaje.setPolylineCasetasIda(ruta.getPolylineCasetasIda());
        viaje.setPolylineCasetasRegreso(ruta.getPolylineCasetasRegreso());
        viaje.setPolylineLibreIda(ruta.getPolylineLibreIda());
        viaje.setPolylineLibreRegreso(ruta.getPolylineLibreRegreso());
        poblarDestinosDesdeRuta(viaje, ruta);
        copiarCasetasDeRuta(viaje, ruta);
    }

    private void poblarDestinosDesdeRuta(Viaje viaje, Ruta ruta) {
        int orden = 0;
        for (Destino d : List.of(ruta.getOrigen(), ruta.getDestino())) {
            viaje.getViajeDestinos().add(ViajeDestino.builder()
                    .viaje(viaje)
                    .nombre(d.getNombre())
                    .direccion(d.getDireccion())
                    .latitud(d.getLatitud())
                    .longitud(d.getLongitud())
                    .orden(orden)
                    .visitado(orden == 0)
                    .build());
            orden++;
        }
    }

    private void copiarCasetasDeRuta(Viaje viaje, Ruta ruta) {
        if (viaje.getTipoTrayecto() != TipoTrayecto.CASETAS || ruta.getCasetas() == null) {
            return;
        }
        int orden = 1;
        for (Caseta toll : ruta.getCasetas().stream()
                .sorted(Comparator.comparing(Caseta::getOrden))
                .toList()) {
            viaje.getViajeCasetas().add(ViajeCaseta.builder()
                    .viaje(viaje)
                    .nombre(toll.getNombre())
                    .costo(toll.getCosto() == null ? 0.0 : toll.getCosto())
                    .orden(orden++)
                    .latitud(toll.getLatitud())
                    .longitud(toll.getLongitud())
                    .build());
        }
    }

    private List<Destino> resolverDestinos(ViajeRequest request) {
        Destino origen = destinoService.getDestino(request.idOrigen());
        if (!origen.getActivo()) {
            throw new InvalidEntryException("El origen debe estar activo");
        }
        List<Destino> destinos = new ArrayList<>();
        destinos.add(origen);
        for (Integer id : request.idDestinos()) {
            Destino destino = destinoService.getDestino(id);
            if (!destino.getActivo()) {
                throw new InvalidEntryException("El destino " + destino.getNombre() + " está inactivo");
            }
            if (destino.getIdDestino().equals(origen.getIdDestino())) {
                throw new InvalidEntryException("El origen no puede repetirse como destino");
            }
            if (destinos.stream().anyMatch(d -> d.getIdDestino().equals(destino.getIdDestino()))) {
                throw new InvalidEntryException("Hay destinos duplicados en la solicitud");
            }
            destinos.add(destino);
        }
        return destinos;
    }

    private void poblarDestinos(Viaje viaje, List<Destino> destinos) {
        int orden = 0;
        for (Destino d : destinos) {
            viaje.getViajeDestinos().add(ViajeDestino.builder()
                    .viaje(viaje)
                    .nombre(d.getNombre())
                    .direccion(d.getDireccion())
                    .latitud(d.getLatitud())
                    .longitud(d.getLongitud())
                    .orden(orden)
                    .visitado(orden == 0)
                    .build());
            orden++;
        }
    }

    private void poblarCasetas(Viaje viaje, List<GoogleMapsService.CasetaToll> casetas) {
        if (viaje.getTipoTrayecto() != TipoTrayecto.CASETAS || casetas == null) {
            return;
        }
        int orden = 1;
        for (GoogleMapsService.CasetaToll toll : casetas) {
            viaje.getViajeCasetas().add(ViajeCaseta.builder()
                    .viaje(viaje)
                    .nombre(toll.nombre())
                    .costo(toll.costo() == null ? 0.0 : toll.costo())
                    .orden(orden++)
                    .latitud(toll.latitud())
                    .longitud(toll.longitud())
                    .build());
        }
    }

    private void validarFechas(LocalDateTime salida, LocalDateTime regreso) {
        if (regreso.isBefore(salida)) {
            throw new InvalidEntryException("La fecha de regreso no puede ser anterior a la fecha de salida");
        }
    }

    private void validarSinTraslapes(Integer idVehiculo, Integer idChofer,
                                     LocalDateTime salida, LocalDateTime regreso, Integer idViaje) {
        List<ViajeStatus> activos = List.of(ViajeStatus.PROXIMO, ViajeStatus.EN_VIAJE);
        if (!viajeRepository.findConflictingByVehiculo(idVehiculo, salida, regreso, activos, idViaje).isEmpty()) {
            throw new InvalidEntryException("El vehículo ya tiene un viaje programado en ese rango de fechas");
        }
        if (!viajeRepository.findConflictingByChofer(idChofer, salida, regreso, activos, idViaje).isEmpty()) {
            throw new InvalidEntryException("El chofer ya tiene un viaje programado en ese rango de fechas");
        }
    }

    private Vehiculo getVehiculoDisponible(Integer id) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehículo no encontrado con id " + id));
        if (!vehiculo.getActivo()) {
            throw new InvalidEntryException("El vehículo está inactivo");
        }
        if (vehiculo.getStatus() == VehiculoStatus.MANTENIMIENTO
                || vehiculo.getStatus() == VehiculoStatus.FUERA_SERVICIO) {
            throw new InvalidEntryException("El vehículo se encuentra en " + vehiculo.getStatus().name().replace("_", " "));
        }
        return vehiculo;
    }

    private Persona getChofer(Integer id) {
        Persona chofer = personaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chofer no encontrado con id " + id));
        if (chofer.getRole() != Role.CHOFER) {
            throw new InvalidEntryException("La persona seleccionada no tiene rol de chofer");
        }
        if (!chofer.getActiva()) {
            throw new InvalidEntryException("El chofer está inactivo");
        }
        return chofer;
    }

    private void liberarVehiculo(Vehiculo vehiculo) {
        long activos = viajeRepository.countByVehiculoIdVehiculoAndStatusIn(vehiculo.getIdVehiculo(),
                List.of(ViajeStatus.PROXIMO, ViajeStatus.EN_VIAJE));
        if (activos == 0 && vehiculo.getStatus() == VehiculoStatus.EN_SERVICIO) {
            vehiculo.setStatus(VehiculoStatus.DISPONIBLE);
            vehiculoRepository.save(vehiculo);
        }
    }

    @Transactional
    public void recalculcarDisponibilidad(Vehiculo vehiculo) {
        if (vehiculo.getStatus() == VehiculoStatus.MANTENIMIENTO
                || vehiculo.getStatus() == VehiculoStatus.FUERA_SERVICIO) {
            return;
        }
        long activos = viajeRepository.countByVehiculoIdVehiculoAndStatusIn(vehiculo.getIdVehiculo(),
                List.of(ViajeStatus.PROXIMO, ViajeStatus.EN_VIAJE));
        vehiculo.setStatus(activos == 0 ? VehiculoStatus.DISPONIBLE : VehiculoStatus.EN_SERVICIO);
        vehiculoRepository.save(vehiculo);
    }

    private VehiculoStatus disponibilidadVehiculo(Vehiculo vehiculo) {
        long activos = viajeRepository.countByVehiculoIdVehiculoAndStatusIn(vehiculo.getIdVehiculo(),
                List.of(ViajeStatus.PROXIMO, ViajeStatus.EN_VIAJE));
        return activos == 0 ? VehiculoStatus.DISPONIBLE : VehiculoStatus.EN_SERVICIO;
    }

    public Viaje getViaje(Integer id) {
        return viajeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Viaje no encontrado con id " + id));
    }

    private ViajeResponse toResponse(Viaje viaje) {
        Vehiculo vehiculo = viaje.getVehiculo();
        Persona chofer = viaje.getChofer();

        int totalDias = (int) ChronoUnit.DAYS.between(
                viaje.getFechaSalida().toLocalDate(), viaje.getFechaRegreso().toLocalDate()) + 1;
        double totalViaticos = (costoComida(viaje) + costoHospedaje(viaje))
                * totalDias * viaje.getNumeroPasajeros();
        double totalCasetas = totalCasetas(viaje);
        double totalViaje = totalViaticos + totalCasetas;

        double litrosConsumidos = litrosConsumidos(viaje, vehiculo);
        double costoCombustible = costoCombustible(litrosConsumidos);
        boolean alertaGasolinaBaja = alertaGasolinaBaja(vehiculo, litrosConsumidos,
                viaje.getStatus() == ViajeStatus.PROXIMO || viaje.getStatus() == ViajeStatus.EN_VIAJE);

        return new ViajeResponse(
                viaje.getIdViaje(),
                viaje.getRuta() != null ? viaje.getRuta().getIdRuta() : null,
                nombreRuta(viaje),
                origenRuta(viaje),
                destinoRuta(viaje),
                vehiculo.getIdVehiculo(),
                vehiculo.getPlaca(),
                vehiculo.getMarca() + " " + vehiculo.getModelo(),
                chofer.getIdPersona(),
                chofer.getNombre() + " " + chofer.getApellidoPaterno(),
                viaje.getNumeroPasajeros(),
                vehiculo.getLimitePasajeros(),
                viaje.getExcesoCapacidad(),
                viaje.getFechaSalida(),
                viaje.getFechaRegreso(),
                viaje.getTipoTrayecto(),
                viaje.getStatus(),
                totalDias,
                Math.round(totalCasetas * 100.0) / 100.0,
                Math.round(totalViaticos * 100.0) / 100.0,
                Math.round(totalViaje * 100.0) / 100.0,
                litrosConsumidos,
                costoCombustible,
                alertaGasolinaBaja,
                distanciaKm(viaje),
                duracionMinutos(viaje),
                costoComida(viaje),
                costoHospedaje(viaje),
                polylineCasetas(viaje),
                polylineLibre(viaje),
                viaje.getPolylineCasetasIda(),
                viaje.getPolylineCasetasRegreso(),
                viaje.getPolylineLibreIda(),
                viaje.getPolylineLibreRegreso(),
                destinosResponse(viaje),
                casetasResponse(viaje),
                viaje.getFechaCreacion(),
                Math.round(vehiculo.getNivelCombustible() * 100.0) / 100.0,
                Math.round((litrosConsumidos + reservaCombustible(vehiculo)) * 100.0) / 100.0,
                Math.round(combustibleFaltante(vehiculo, litrosConsumidos) * 100.0) / 100.0
        );
    }

    private double distanciaKm(Viaje viaje) {
        return viaje.getDistanciaKm() != null ? viaje.getDistanciaKm()
                : (viaje.getRuta() != null ? viaje.getRuta().getDistanciaKm() : 0.0);
    }

    private double duracionMinutos(Viaje viaje) {
        return viaje.getDuracionMinutos() != null ? viaje.getDuracionMinutos()
                : (viaje.getRuta() != null ? viaje.getRuta().getDuracionMinutos() : 0.0);
    }

    private double costoComida(Viaje viaje) {
        return viaje.getCostoComida() != null ? viaje.getCostoComida()
                : (viaje.getRuta() != null ? viaje.getRuta().getCostoComida() : 0.0);
    }

    private double costoHospedaje(Viaje viaje) {
        return viaje.getCostoHospedaje() != null ? viaje.getCostoHospedaje()
                : (viaje.getRuta() != null ? viaje.getRuta().getCostoHospedaje() : 0.0);
    }

    private String polylineCasetas(Viaje viaje) {
        return viaje.getPolylineCasetas() != null ? viaje.getPolylineCasetas()
                : (viaje.getRuta() != null ? viaje.getRuta().getPolylineCasetas() : null);
    }

    private String polylineLibre(Viaje viaje) {
        return viaje.getPolylineLibre() != null ? viaje.getPolylineLibre()
                : (viaje.getRuta() != null ? viaje.getRuta().getPolylineLibre() : null);
    }

    private double totalCasetas(Viaje viaje) {
        if (viaje.getTipoTrayecto() != TipoTrayecto.CASETAS) {
            return 0.0;
        }
        if (viaje.getViajeCasetas() != null && !viaje.getViajeCasetas().isEmpty()) {
            double suma = viaje.getViajeCasetas().stream()
                    .mapToDouble(ViajeCaseta::getCosto)
                    .sum();
            return Math.round(suma * 100.0) / 100.0;
        }
        if (viaje.getRuta() != null) {
            return rutaService.costoTotalCasetas(viaje.getRuta());
        }
        return 0.0;
    }

    public String nombreRuta(Viaje viaje) {
        if (viaje.getNombreRutaSnapshot() != null) {
            return viaje.getNombreRutaSnapshot();
        }
        if (viaje.getRuta() != null) {
            return viaje.getRuta().getNombre();
        }
        List<ViajeDestino> destinos = ordenados(viaje.getViajeDestinos());
        if (destinos.isEmpty()) {
            return null;
        }
        return destinos.size() > 1
                ? destinos.get(0).getNombre() + " - " + destinos.get(destinos.size() - 1).getNombre()
                : destinos.get(0).getNombre();
    }

    public String origenRuta(Viaje viaje) {
        if (viaje.getRuta() != null) {
            return viaje.getRuta().getOrigen().getNombre();
        }
        List<ViajeDestino> destinos = ordenados(viaje.getViajeDestinos());
        return destinos.isEmpty() ? null : destinos.get(0).getNombre();
    }

    public String destinoRuta(Viaje viaje) {
        if (viaje.getRuta() != null) {
            return viaje.getRuta().getDestino().getNombre();
        }
        List<ViajeDestino> destinos = ordenados(viaje.getViajeDestinos());
        return destinos.isEmpty() ? null : destinos.get(destinos.size() - 1).getNombre();
    }

    private List<ViajeDestinoResponse> destinosResponse(Viaje viaje) {
        if (viaje.getViajeDestinos() != null && !viaje.getViajeDestinos().isEmpty()) {
            return ordenados(viaje.getViajeDestinos()).stream()
                    .map(d -> new ViajeDestinoResponse(
                            d.getIdViajeDestino(),
                            d.getNombre(),
                            d.getDireccion(),
                            d.getLatitud(),
                            d.getLongitud(),
                            d.getOrden(),
                            d.getVisitado(),
                            d.getFechaLlegada() != null ? d.getFechaLlegada().toString() : null))
                    .toList();
        }
        if (viaje.getRuta() != null) {
            return List.of(new ViajeDestinoResponse(
                    null,
                    viaje.getRuta().getDestino().getNombre(),
                    viaje.getRuta().getDestino().getDireccion(),
                    viaje.getRuta().getDestino().getLatitud(),
                    viaje.getRuta().getDestino().getLongitud(),
                    1,
                    null,
                    null));
        }
        return List.of();
    }

    private List<ViajeCasetaResponse> casetasResponse(Viaje viaje) {
        if (viaje.getViajeCasetas() != null && !viaje.getViajeCasetas().isEmpty()) {
            return viaje.getViajeCasetas().stream()
                    .sorted(Comparator.comparing(ViajeCaseta::getOrden))
                    .map(c -> new ViajeCasetaResponse(
                            c.getIdViajeCaseta(),
                            c.getNombre(),
                            c.getCosto(),
                            c.getOrden(),
                            c.getLatitud(),
                            c.getLongitud()))
                    .toList();
        }
        if (viaje.getRuta() != null) {
            return viaje.getRuta().getCasetas().stream()
                    .sorted(Comparator.comparing(Caseta::getOrden))
                    .map(c -> new ViajeCasetaResponse(
                            null,
                            c.getNombre(),
                            c.getCosto(),
                            c.getOrden(),
                            c.getLatitud(),
                            c.getLongitud()))
                    .toList();
        }
        return List.of();
    }

    private List<ViajeDestino> ordenados(List<ViajeDestino> destinos) {
        if (destinos == null || destinos.isEmpty()) {
            return List.of();
        }
        return destinos.stream()
                .sorted(Comparator.comparing(ViajeDestino::getOrden))
                .toList();
    }

    private double litrosConsumidos(Viaje viaje, Vehiculo vehiculo) {
        double litros = distanciaKm(viaje) / vehiculo.getRendimiento();
        return Math.round(litros * 100.0) / 100.0;
    }

    private double costoCombustible(double litrosConsumidos) {
        double precioLitro = configuracionService.getDouble(ConfiguracionService.PRECIO_COMBUSTIBLE, 0.0);
        return Math.round(litrosConsumidos * precioLitro * 100.0) / 100.0;
    }

    private boolean alertaGasolinaBaja(Vehiculo vehiculo, double litrosConsumidos, boolean prediccion) {
        return prediccion
                ? combustibleFaltante(vehiculo, litrosConsumidos) > 0
                : vehiculo.getNivelCombustible() < reservaCombustible(vehiculo);
    }

    private double reservaCombustible(Vehiculo vehiculo) {
        double umbralPct = configuracionService.getDouble(ConfiguracionService.UMBRAL_GASOLINA, 20.0);
        return vehiculo.getCapacidadTanqueLitros() * umbralPct / 100.0;
    }

    private double combustibleFaltante(Vehiculo vehiculo, double litrosConsumidos) {
        return Math.max(0.0, litrosConsumidos + reservaCombustible(vehiculo) - vehiculo.getNivelCombustible());
    }

    public FilaFinanciera filaFinanciera(Viaje viaje) {
        Vehiculo vehiculo = viaje.getVehiculo();
        int totalDias = (int) ChronoUnit.DAYS.between(
                viaje.getFechaSalida().toLocalDate(), viaje.getFechaRegreso().toLocalDate()) + 1;
        double totalViaticos = (costoComida(viaje) + costoHospedaje(viaje))
                * totalDias * viaje.getNumeroPasajeros();
        double totalCasetas = totalCasetas(viaje);
        double litros = litrosConsumidos(viaje, vehiculo);
        double costoCombustible = costoCombustible(litros);
        double totalViaje = totalViaticos + totalCasetas;
        return new FilaFinanciera(totalDias,
                Math.round(totalCasetas * 100.0) / 100.0,
                Math.round(totalViaticos * 100.0) / 100.0,
                litros,
                costoCombustible,
                Math.round(totalViaje * 100.0) / 100.0);
    }

    public record FilaFinanciera(int totalDias, double totalCasetas, double totalViaticos,
                                 double litros, double costoCombustible, double totalViaje) {
    }
}
