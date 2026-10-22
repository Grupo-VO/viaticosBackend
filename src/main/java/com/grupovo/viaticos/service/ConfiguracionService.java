package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.ConfiguracionSistema;
import com.grupovo.viaticos.dto.ConfiguracionRequest;
import com.grupovo.viaticos.dto.ConfiguracionResponse;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.repository.ConfiguracionSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfiguracionService {

    public static final String PRECIO_COMBUSTIBLE = "PRECIO_COMBUSTIBLE";
    public static final String UMBRAL_GASOLINA = "UMBRAL_GASOLINA";
    public static final String DISTANCIA_ALERTA_METROS = "DISTANCIA_ALERTA_METROS";
    public static final String RADIO_LLEGADA_METROS = "RADIO_LLEGADA_METROS";
    public static final String TIEMPO_SIN_COMUNICACION_SEGUNDOS = "TIEMPO_SIN_COMUNICACION_SEGUNDOS";

    private final ConfiguracionSistemaRepository configuracionRepository;

    public ConfiguracionService(ConfiguracionSistemaRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional(readOnly = true)
    public List<ConfiguracionResponse> listar() {
        return configuracionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracionResponse obtener(Integer id) {
        return toResponse(getConfiguracion(id));
    }

    @Transactional
    public ConfiguracionResponse crear(ConfiguracionRequest request) {
        if (configuracionRepository.findByClave(request.clave().trim()).isPresent()) {
            throw new InvalidEntryException("Ya existe una configuración con la clave " + request.clave());
        }
        ConfiguracionSistema configuracion = ConfiguracionSistema.builder()
                .clave(request.clave().trim().toUpperCase())
                .valor(request.valor().trim())
                .descripcion(request.descripcion())
                .build();
        return toResponse(configuracionRepository.save(configuracion));
    }

    @Transactional
    public ConfiguracionResponse actualizar(Integer id, ConfiguracionRequest request) {
        ConfiguracionSistema configuracion = getConfiguracion(id);
        configuracion.setValor(request.valor().trim());
        if (request.descripcion() != null) {
            configuracion.setDescripcion(request.descripcion());
        }
        return toResponse(configuracionRepository.save(configuracion));
    }

    @Transactional
    public void eliminar(Integer id) {
        configuracionRepository.delete(getConfiguracion(id));
    }

    public ConfiguracionSistema getConfiguracion(Integer id) {
        return configuracionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Configuración no encontrada con id " + id));
    }

    public ConfiguracionSistema getByClave(String clave) {
        return configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new NotFoundException("Configuración no encontrada con clave " + clave));
    }

    public double getDouble(String clave, double valorPorDefecto) {
        return configuracionRepository.findByClave(clave)
                .map(c -> {
                    try {
                        return Double.parseDouble(c.getValor());
                    } catch (NumberFormatException e) {
                        return valorPorDefecto;
                    }
                })
                .orElse(valorPorDefecto);
    }

    private ConfiguracionResponse toResponse(ConfiguracionSistema c) {
        return new ConfiguracionResponse(
                c.getIdConfiguracion(), c.getClave(), c.getValor(),
                c.getDescripcion(), c.getFechaCreacion());
    }
}
