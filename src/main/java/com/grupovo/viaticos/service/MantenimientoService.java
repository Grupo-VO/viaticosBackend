package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Mantenimiento;
import com.grupovo.viaticos.domain.Vehiculo;
import com.grupovo.viaticos.domain.en.MantenimientoEstado;
import com.grupovo.viaticos.dto.MantenimientoRequest;
import com.grupovo.viaticos.dto.MantenimientoResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.MantenimientoRepository;
import com.grupovo.viaticos.repository.ViajeRepository;
import com.grupovo.viaticos.domain.en.ViajeStatus;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final VehiculoService vehiculoService;
    private final ViajeRepository viajeRepository;
    private final ViajeService viajeService;
    private final StorageService storageService;

    public MantenimientoService(MantenimientoRepository mantenimientoRepository,
                                VehiculoService vehiculoService,
                                ViajeRepository viajeRepository,
                                ViajeService viajeService,
                                StorageService storageService) {
        this.mantenimientoRepository = mantenimientoRepository;
        this.vehiculoService = vehiculoService;
        this.viajeRepository = viajeRepository;
        this.viajeService = viajeService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listar() {
        return mantenimientoRepository.findAllByOrderByFechaDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listarPorVehiculo(Integer idVehiculo) {
        return mantenimientoRepository.findAllByVehiculoIdVehiculoOrderByFechaDesc(idVehiculo).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MantenimientoResponse obtener(Integer id) {
        return toResponse(getMantenimiento(id));
    }

    @Transactional
    public MantenimientoResponse crear(MantenimientoRequest request, MultipartFile evidencia) {
        Vehiculo vehiculo = vehiculoService.getVehiculo(request.idVehiculo());
        if (!vehiculo.getActivo()) {
            throw new InvalidEntryException("El vehículo está inactivo");
        }
        if (request.odometro() < vehiculo.getKilometraje()) {
            throw new InvalidEntryException("El odómetro no puede ser menor al kilometraje actual del vehículo");
        }
        if (mantenimientoRepository.existsByVehiculoIdVehiculoAndEstado(
                request.idVehiculo(), MantenimientoEstado.EN_CURSO)) {
            throw new InvalidEntryException("El vehículo ya tiene un mantenimiento en curso");
        }
        if (viajeRepository.countByVehiculoIdVehiculoAndStatus(request.idVehiculo(), ViajeStatus.PROXIMO) > 0) {
            throw new InvalidEntryException("No se puede registrar un mantenimiento con viajes próximos del vehículo");
        }

        String evidenciaRuta = storageService.guardar(evidencia, "mantenimiento");

        Mantenimiento mantenimiento = Mantenimiento.builder()
                .vehiculo(vehiculo)
                .tipo(request.tipo())
                .estado(MantenimientoEstado.EN_CURSO)
                .fecha(request.fecha())
                .descripcion(request.descripcion().trim())
                .costo(request.costo())
                .odometro(request.odometro())
                .evidenciaRuta(evidenciaRuta)
                .build();

        vehiculo.setStatus(com.grupovo.viaticos.domain.en.VehiculoStatus.MANTENIMIENTO);
        vehiculoService.actualizarTanque(vehiculo);

        return toResponse(mantenimientoRepository.save(mantenimiento));
    }

    @Transactional
    public MantenimientoResponse completar(Integer id) {
        Mantenimiento mantenimiento = getMantenimiento(id);
        if (mantenimiento.getEstado() != MantenimientoEstado.EN_CURSO) {
            throw new InvalidEntryException("El mantenimiento ya está completado");
        }
        mantenimiento.setEstado(MantenimientoEstado.COMPLETADO);
        viajeService.recalculcarDisponibilidad(mantenimiento.getVehiculo());
        return toResponse(mantenimientoRepository.save(mantenimiento));
    }

    @Transactional
    public void eliminar(Integer id) {
        Mantenimiento mantenimiento = getMantenimiento(id);
        if (mantenimiento.getEstado() == MantenimientoEstado.EN_CURSO) {
            throw new InvalidEntryException("No se puede eliminar un mantenimiento en curso");
        }
        mantenimientoRepository.delete(mantenimiento);
    }

    @Transactional(readOnly = true)
    public Resource descargarEvidencia(Integer id) {
        return storageService.cargar(getMantenimiento(id).getEvidenciaRuta());
    }

    public Mantenimiento getMantenimiento(Integer id) {
        return mantenimientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mantenimiento no encontrado con id " + id));
    }

    private MantenimientoResponse toResponse(Mantenimiento m) {
        Vehiculo v = m.getVehiculo();
        return new MantenimientoResponse(
                m.getIdMantenimiento(),
                v.getIdVehiculo(),
                v.getPlaca(),
                v.getMarca() + " " + v.getModelo(),
                m.getTipo(),
                m.getEstado(),
                m.getFecha(),
                m.getDescripcion(),
                m.getCosto(),
                m.getOdometro(),
                m.getEvidenciaRuta(),
                m.getFechaCreacion()
        );
    }
}
