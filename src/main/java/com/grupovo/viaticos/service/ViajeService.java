package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Caseta;
import com.grupovo.viaticos.domain.Persona;
import com.grupovo.viaticos.domain.Ruta;
import com.grupovo.viaticos.domain.Vehiculo;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.en.Role;
import com.grupovo.viaticos.domain.en.TipoTrayecto;
import com.grupovo.viaticos.domain.en.VehiculoStatus;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import com.grupovo.viaticos.dto.ViajeRequest;
import com.grupovo.viaticos.dto.ViajeResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.PersonaRepository;
import com.grupovo.viaticos.repository.VehiculoRepository;
import com.grupovo.viaticos.repository.ViajeRepository;
import com.grupovo.viaticos.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ViajeService {

    private final ViajeRepository viajeRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PersonaRepository personaRepository;
    private final RutaService rutaService;
    private final ConfiguracionService configuracionService;

    public ViajeService(ViajeRepository viajeRepository,
                        VehiculoRepository vehiculoRepository,
                        PersonaRepository personaRepository,
                        RutaService rutaService,
                        ConfiguracionService configuracionService) {
        this.viajeRepository = viajeRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.personaRepository = personaRepository;
        this.rutaService = rutaService;
        this.configuracionService = configuracionService;
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
        return toResponse(getViaje(id));
    }

    @Transactional
    public ViajeResponse crear(ViajeRequest request) {
        Ruta ruta = rutaService.getRuta(request.idRuta());
        if (!ruta.getActivo()) {
            throw new InvalidEntryException("La ruta seleccionada está inactiva");
        }
        Vehiculo vehiculo = getVehiculoDisponible(request.idVehiculo());
        Persona chofer = getChofer(request.idChofer());

        validarFechas(request.fechaSalida(), request.fechaRegreso());

        boolean excesoCapacidad = request.numeroPasajeros() > vehiculo.getLimitePasajeros();

        Viaje viaje = Viaje.builder()
                .ruta(ruta)
                .vehiculo(vehiculo)
                .chofer(chofer)
                .numeroPasajeros(request.numeroPasajeros())
                .fechaSalida(request.fechaSalida())
                .fechaRegreso(request.fechaRegreso())
                .tipoTrayecto(request.tipoTrayecto())
                .status(ViajeStatus.PROXIMO)
                .excesoCapacidad(excesoCapacidad)
                .build();

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

        viaje.setRuta(rutaService.getRuta(request.idRuta()));
        viaje.setVehiculo(vehiculo);
        viaje.setChofer(chofer);
        viaje.setNumeroPasajeros(request.numeroPasajeros());
        viaje.setFechaSalida(request.fechaSalida());
        viaje.setFechaRegreso(request.fechaRegreso());
        viaje.setTipoTrayecto(request.tipoTrayecto());
        viaje.setExcesoCapacidad(request.numeroPasajeros() > vehiculo.getLimitePasajeros());

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
        if (viaje.getStatus() != ViajeStatus.PROXIMO) {
            throw new InvalidEntryException("Solo se pueden completar viajes en estado PRÓXIMO");
        }
        viaje.setStatus(ViajeStatus.COMPLETADO);

        Vehiculo vehiculo = viaje.getVehiculo();
        double litrosConsumidos = litrosConsumidos(viaje.getRuta(), vehiculo);
        vehiculo.setNivelCombustible(Math.max(0.0, vehiculo.getNivelCombustible() - litrosConsumidos));
        vehiculo.setKilometraje(vehiculo.getKilometraje() + viaje.getRuta().getDistanciaKm());
        vehiculoRepository.save(vehiculo);

        liberarVehiculo(vehiculo);
        return toResponse(viajeRepository.save(viaje));
    }

    private void validarFechas(LocalDateTime salida, LocalDateTime regreso) {
        if (regreso.isBefore(salida)) {
            throw new InvalidEntryException("La fecha de regreso no puede ser anterior a la fecha de salida");
        }
    }

    private void validarSinTraslapes(Integer idVehiculo, Integer idChofer,
                                     LocalDateTime salida, LocalDateTime regreso, Integer idViaje) {
        if (!viajeRepository.findConflictingByVehiculo(idVehiculo, salida, regreso, ViajeStatus.PROXIMO, idViaje).isEmpty()) {
            throw new InvalidEntryException("El vehículo ya tiene un viaje programado en ese rango de fechas");
        }
        if (!viajeRepository.findConflictingByChofer(idChofer, salida, regreso, ViajeStatus.PROXIMO, idViaje).isEmpty()) {
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
        long activos = viajeRepository.countByVehiculoIdVehiculoAndStatus(vehiculo.getIdVehiculo(), ViajeStatus.PROXIMO);
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
        long activos = viajeRepository.countByVehiculoIdVehiculoAndStatus(vehiculo.getIdVehiculo(), ViajeStatus.PROXIMO);
        vehiculo.setStatus(activos == 0 ? VehiculoStatus.DISPONIBLE : VehiculoStatus.EN_SERVICIO);
        vehiculoRepository.save(vehiculo);
    }

    private VehiculoStatus disponibilidadVehiculo(Vehiculo vehiculo) {
        long activos = viajeRepository.countByVehiculoIdVehiculoAndStatus(vehiculo.getIdVehiculo(), ViajeStatus.PROXIMO);
        return activos == 0 ? VehiculoStatus.DISPONIBLE : VehiculoStatus.EN_SERVICIO;
    }

    public Viaje getViaje(Integer id) {
        return viajeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Viaje no encontrado con id " + id));
    }

    private ViajeResponse toResponse(Viaje viaje) {
        Ruta ruta = viaje.getRuta();
        Vehiculo vehiculo = viaje.getVehiculo();
        Persona chofer = viaje.getChofer();

        int totalDias = (int) ChronoUnit.DAYS.between(
                viaje.getFechaSalida().toLocalDate(), viaje.getFechaRegreso().toLocalDate()) + 1;
        double totalViaticos = (ruta.getCostoComida() + ruta.getCostoHospedaje()) * totalDias;
        double totalCasetas = viaje.getTipoTrayecto() == TipoTrayecto.CASETAS
                ? rutaService.costoTotalCasetas(ruta)
                : 0.0;
        double totalViaje = totalViaticos + totalCasetas;

        double litrosConsumidos = litrosConsumidos(ruta, vehiculo);
        double costoCombustible = costoCombustible(litrosConsumidos);
        boolean alertaGasolinaBaja = alertaGasolinaBaja(vehiculo, litrosConsumidos,
                viaje.getStatus() == ViajeStatus.PROXIMO);

        return new ViajeResponse(
                viaje.getIdViaje(),
                ruta.getIdRuta(),
                ruta.getNombre(),
                ruta.getOrigen().getNombre(),
                ruta.getDestino().getNombre(),
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
                viaje.getFechaCreacion()
        );
    }

    private double litrosConsumidos(Ruta ruta, Vehiculo vehiculo) {
        double litros = ruta.getDistanciaKm() / vehiculo.getRendimiento();
        return Math.round(litros * 100.0) / 100.0;
    }

    private double costoCombustible(double litrosConsumidos) {
        double precioLitro = configuracionService.getDouble(ConfiguracionService.PRECIO_COMBUSTIBLE, 0.0);
        return Math.round(litrosConsumidos * precioLitro * 100.0) / 100.0;
    }

    private boolean alertaGasolinaBaja(Vehiculo vehiculo, double litrosConsumidos, boolean prediccion) {
        double umbralPct = configuracionService.getDouble(ConfiguracionService.UMBRAL_GASOLINA, 20.0);
        double nivelEfectivo = prediccion
                ? vehiculo.getNivelCombustible() - litrosConsumidos
                : vehiculo.getNivelCombustible();
        double umbralLitros = vehiculo.getCapacidadTanqueLitros() * umbralPct / 100.0;
        return nivelEfectivo < umbralLitros;
    }
}
