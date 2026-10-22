package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.CargaCombustible;
import com.grupovo.viaticos.domain.Vehiculo;
import com.grupovo.viaticos.dto.CargaCombustibleRequest;
import com.grupovo.viaticos.dto.CargaCombustibleResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.CargaCombustibleRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class CargaCombustibleService {

    private final CargaCombustibleRepository cargaRepository;
    private final VehiculoService vehiculoService;
    private final ConfiguracionService configuracionService;
    private final StorageService storageService;

    public CargaCombustibleService(CargaCombustibleRepository cargaRepository,
                                   VehiculoService vehiculoService,
                                   ConfiguracionService configuracionService,
                                   StorageService storageService) {
        this.cargaRepository = cargaRepository;
        this.vehiculoService = vehiculoService;
        this.configuracionService = configuracionService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<CargaCombustibleResponse> listar() {
        return cargaRepository.findAllByOrderByFechaDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CargaCombustibleResponse> listarPorVehiculo(Integer idVehiculo) {
        return cargaRepository.findAllByVehiculoIdVehiculoOrderByFechaDesc(idVehiculo).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CargaCombustibleResponse obtener(Integer id) {
        return toResponse(getCarga(id));
    }

    @Transactional
    public CargaCombustibleResponse crear(CargaCombustibleRequest request, MultipartFile comprobante) {
        Vehiculo vehiculo = vehiculoService.getVehiculo(request.idVehiculo());
        if (!vehiculo.getActivo()) {
            throw new InvalidEntryException("El vehículo está inactivo");
        }
        if (request.odometro() < vehiculo.getKilometraje()) {
            throw new InvalidEntryException("El odómetro no puede ser menor al kilometraje actual del vehículo");
        }
        double combustibleDisponible = vehiculo.getCapacidadTanqueLitros() - vehiculo.getNivelCombustible();
        if (request.litros() > combustibleDisponible + 0.0001) {
            throw new InvalidEntryException("La carga excede el espacio disponible del tanque (" +
                    Math.round(combustibleDisponible * 100.0) / 100.0 + " L)");
        }

        double precioLitro = configuracionService.getDouble(ConfiguracionService.PRECIO_COMBUSTIBLE, 0.0);
        double costoTotal = Math.round(request.litros() * precioLitro * 100.0) / 100.0;
        String comprobanteRuta = storageService.guardar(comprobante, "combustible");

        CargaCombustible carga = CargaCombustible.builder()
                .vehiculo(vehiculo)
                .fecha(request.fecha())
                .litros(request.litros())
                .costoTotal(costoTotal)
                .odometro(request.odometro())
                .comprobanteRuta(comprobanteRuta)
                .build();

        double nivelActualizado = vehiculo.getNivelCombustible() + request.litros();
        vehiculo.setNivelCombustible(nivelActualizado);
        vehiculo.setKilometraje(Math.max(vehiculo.getKilometraje(), request.odometro()));
        vehiculoService.actualizarTanque(vehiculo);

        return toResponse(cargaRepository.save(carga));
    }

    @Transactional(readOnly = true)
    public Resource descargarComprobante(Integer id) {
        return storageService.cargar(getCarga(id).getComprobanteRuta());
    }

    public CargaCombustible getCarga(Integer id) {
        return cargaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Carga de combustible no encontrada con id " + id));
    }

    private CargaCombustibleResponse toResponse(CargaCombustible c) {
        Vehiculo v = c.getVehiculo();
        return new CargaCombustibleResponse(
                c.getIdCarga(),
                v.getIdVehiculo(),
                v.getPlaca(),
                v.getMarca() + " " + v.getModelo(),
                c.getFecha(),
                c.getLitros(),
                c.getCostoTotal(),
                c.getOdometro(),
                c.getComprobanteRuta(),
                c.getFechaCreacion()
        );
    }
}
