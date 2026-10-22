package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Destino;
import com.grupovo.viaticos.dto.DestinoRequest;
import com.grupovo.viaticos.dto.DestinoResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.DestinoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DestinoService {

    private final DestinoRepository destinoRepository;

    public DestinoService(DestinoRepository destinoRepository) {
        this.destinoRepository = destinoRepository;
    }

    @Transactional(readOnly = true)
    public List<DestinoResponse> listar() {
        return destinoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DestinoResponse obtener(Integer id) {
        return toResponse(getDestino(id));
    }

    @Transactional
    public DestinoResponse crear(DestinoRequest request) {
        String nombre = normalizar(request.nombre());
        validarCoordenadas(request.latitud(), request.longitud());
        validarNombreDisponible(nombre, null);
        Destino destino = Destino.builder()
                .nombre(nombre)
                .direccion(request.direccion().trim())
                .latitud(request.latitud())
                .longitud(request.longitud())
                .activo(true)
                .build();
        return toResponse(destinoRepository.save(destino));
    }

    @Transactional
    public DestinoResponse actualizar(Integer id, DestinoRequest request) {
        Destino destino = getDestino(id);
        String nombre = normalizar(request.nombre());
        validarCoordenadas(request.latitud(), request.longitud());
        validarNombreDisponible(nombre, id);
        destino.setNombre(nombre);
        destino.setDireccion(request.direccion().trim());
        destino.setLatitud(request.latitud());
        destino.setLongitud(request.longitud());
        return toResponse(destinoRepository.save(destino));
    }

    @Transactional
    public void cambiarEstado(Integer id, Boolean activo) {
        if (activo == null) {
            throw new InvalidEntryException("El estado activo es obligatorio");
        }
        Destino destino = getDestino(id);
        destino.setActivo(activo);
        destinoRepository.save(destino);
    }

    public Destino getDestino(Integer id) {
        return destinoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Destino no encontrado con id " + id));
    }

    private DestinoResponse toResponse(Destino d) {
        return new DestinoResponse(
                d.getIdDestino(),
                d.getNombre(),
                d.getDireccion(),
                d.getLatitud(),
                d.getLongitud(),
                d.getActivo()
        );
    }

    private String normalizar(String valor) {
        return valor.trim();
    }

    private void validarCoordenadas(Double latitud, Double longitud) {
        if (latitud == null || latitud < -90 || latitud > 90
                || longitud == null || longitud < -180 || longitud > 180) {
            throw new InvalidEntryException("Las coordenadas del destino no son válidas");
        }
    }

    private void validarNombreDisponible(String nombre, Integer idDestino) {
        boolean existe = idDestino == null
                ? destinoRepository.existsByNombreIgnoreCase(nombre)
                : destinoRepository.existsByNombreIgnoreCaseAndIdDestinoNot(nombre, idDestino);
        if (existe) {
            throw new InvalidEntryException("Ya existe un destino con ese nombre");
        }
    }
}
