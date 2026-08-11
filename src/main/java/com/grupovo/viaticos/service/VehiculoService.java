package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Vehiculo;
import com.grupovo.viaticos.domain.en.VehiculoStatus;
import com.grupovo.viaticos.dto.VehiculoRequest;
import com.grupovo.viaticos.dto.VehiculoResponse;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar() {
        return vehiculoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtener(Integer id) {
        return toResponse(getVehiculo(id));
    }

    @Transactional
    public VehiculoResponse crear(VehiculoRequest request) {
        Vehiculo vehiculo = Vehiculo.builder()
                .marca(request.marca().trim())
                .modelo(request.modelo().trim())
                .placa(request.placa().trim())
                .kilometraje(request.kilometraje())
                .rendimiento(request.rendimiento())
                .limitePasajeros(request.limitePasajeros())
                .capacidadTanqueLitros(request.capacidadTanqueLitros())
                .nivelCombustible(request.capacidadTanqueLitros())
                .status(request.status())
                .activo(true)
                .build();
        return toResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public VehiculoResponse actualizar(Integer id, VehiculoRequest request) {
        Vehiculo vehiculo = getVehiculo(id);
        vehiculo.setMarca(request.marca().trim());
        vehiculo.setModelo(request.modelo().trim());
        vehiculo.setPlaca(request.placa().trim());
        vehiculo.setKilometraje(request.kilometraje());
        vehiculo.setRendimiento(request.rendimiento());
        vehiculo.setLimitePasajeros(request.limitePasajeros());
        vehiculo.setCapacidadTanqueLitros(request.capacidadTanqueLitros());
        vehiculo.setNivelCombustible(Math.min(vehiculo.getNivelCombustible(), request.capacidadTanqueLitros()));
        vehiculo.setStatus(request.status());
        return toResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public VehiculoResponse cambiarEstado(Integer id, Boolean activo) {
        Vehiculo vehiculo = getVehiculo(id);
        vehiculo.setActivo(activo);
        return toResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public VehiculoResponse cambiarStatus(Integer id, VehiculoStatus status) {
        Vehiculo vehiculo = getVehiculo(id);
        vehiculo.setStatus(status);
        return toResponse(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public void actualizarTanque(Vehiculo vehiculo) {
        vehiculoRepository.save(vehiculo);
    }

    public Vehiculo getVehiculo(Integer id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehículo no encontrado con id " + id));
    }

    private VehiculoResponse toResponse(Vehiculo v) {
        return new VehiculoResponse(
                v.getIdVehiculo(),
                v.getMarca(),
                v.getModelo(),
                v.getPlaca(),
                v.getKilometraje(),
                v.getRendimiento(),
                v.getLimitePasajeros(),
                v.getCapacidadTanqueLitros(),
                v.getNivelCombustible(),
                v.getActivo(),
                v.getStatus(),
                v.getFechaCreacion()
        );
    }
}
