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
        Destino destino = Destino.builder()
                .nombre(request.nombre().trim())
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
        destino.setNombre(request.nombre().trim());
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
}
