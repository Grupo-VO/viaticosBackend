package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.Caseta;
import com.grupovo.viaticos.domain.Destino;
import com.grupovo.viaticos.domain.Ruta;
import com.grupovo.viaticos.dto.CasetaResponse;
import com.grupovo.viaticos.dto.DestinoResponse;
import com.grupovo.viaticos.dto.RutaRequest;
import com.grupovo.viaticos.dto.RutaResponse;
import com.grupovo.viaticos.dto.RutaTrazoRequest;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import com.grupovo.viaticos.gmaps.GoogleMapsService;
import com.grupovo.viaticos.repository.RutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;
    private final DestinoService destinoService;
    private final GoogleMapsService googleMapsService;

    public RutaService(RutaRepository rutaRepository,
                       DestinoService destinoService,
                       GoogleMapsService googleMapsService) {
        this.rutaRepository = rutaRepository;
        this.destinoService = destinoService;
        this.googleMapsService = googleMapsService;
    }

    @Transactional(readOnly = true)
    public List<RutaResponse> listar() {
        return rutaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RutaResponse obtener(Integer id) {
        return toResponse(getRuta(id));
    }

    @Transactional
    public RutaResponse crear(RutaRequest request) {
        Destino origen = destinoService.getDestino(request.idOrigen());
        Destino destino = destinoService.getDestino(request.idDestino());
        validarDestinos(request.idOrigen(), request.idDestino());
        validarDestinosActivos(origen, destino);

        GoogleMapsService.RouteData routeData = googleMapsService.calcularRutaRedonda(origen, destino);

        Ruta ruta = Ruta.builder()
                .nombre(request.nombre().trim())
                .origen(origen)
                .destino(destino)
                .usaCasetas(request.usaCasetas())
                .duracionMinutos(routeData.duracionMinutos())
                .distanciaKm(routeData.distanciaKm())
                .costoComida(request.costoComida())
                .costoHospedaje(request.costoHospedaje())
                 .polylineCasetas(routeData.polylineCasetas())
                 .polylineLibre(routeData.polylineLibre())
                 .polylineCasetasIda(routeData.polylineCasetasIda())
                 .polylineCasetasRegreso(routeData.polylineCasetasRegreso())
                 .polylineLibreIda(routeData.polylineLibreIda())
                 .polylineLibreRegreso(routeData.polylineLibreRegreso())
                .activo(true)
                .build();

        List<Caseta> casetas = new ArrayList<>();
        if (request.usaCasetas()) {
            int orden = 1;
            for (GoogleMapsService.CasetaToll toll : routeData.casetas()) {
                casetas.add(Caseta.builder()
                        .ruta(ruta)
                        .nombre(toll.nombre())
                        .costo(toll.costo() == null ? 0.0 : toll.costo())
                        .orden(orden++)
                        .latitud(toll.latitud())
                        .longitud(toll.longitud())
                        .build());
            }
        }
        ruta.setCasetas(casetas);

        return toResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse actualizar(Integer id, RutaRequest request) {
        Ruta ruta = getRuta(id);
        validarDestinos(request.idOrigen(), request.idDestino());
        Destino origen = destinoService.getDestino(request.idOrigen());
        Destino destino = destinoService.getDestino(request.idDestino());
        validarDestinosActivos(origen, destino);
        boolean recalc = !ruta.getOrigen().getIdDestino().equals(request.idOrigen())
                || !ruta.getDestino().getIdDestino().equals(request.idDestino())
                || !ruta.getUsaCasetas().equals(request.usaCasetas());

        ruta.setNombre(request.nombre().trim());
        ruta.setCostoComida(request.costoComida());
        ruta.setCostoHospedaje(request.costoHospedaje());

        if (recalc) {
            ruta.setOrigen(origen);
            ruta.setDestino(destino);
            ruta.setUsaCasetas(request.usaCasetas());

            GoogleMapsService.RouteData routeData = googleMapsService.calcularRutaRedonda(origen, destino);
            ruta.setDuracionMinutos(routeData.duracionMinutos());
            ruta.setDistanciaKm(routeData.distanciaKm());
            ruta.setPolylineCasetas(routeData.polylineCasetas());
            ruta.setPolylineLibre(routeData.polylineLibre());
            ruta.setPolylineCasetasIda(routeData.polylineCasetasIda());
            ruta.setPolylineCasetasRegreso(routeData.polylineCasetasRegreso());
            ruta.setPolylineLibreIda(routeData.polylineLibreIda());
            ruta.setPolylineLibreRegreso(routeData.polylineLibreRegreso());

            ruta.getCasetas().clear();
            if (request.usaCasetas()) {
                int orden = 1;
                for (GoogleMapsService.CasetaToll toll : routeData.casetas()) {
                    ruta.getCasetas().add(Caseta.builder()
                            .ruta(ruta)
                            .nombre(toll.nombre())
                            .costo(toll.costo() == null ? 0.0 : toll.costo())
                            .orden(orden++)
                            .latitud(toll.latitud())
                            .longitud(toll.longitud())
                            .build());
                }
            }
        }

        return toResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse refrescar(Integer id) {
        Ruta ruta = getRuta(id);
        GoogleMapsService.RouteData routeData =
                googleMapsService.calcularRutaRedonda(ruta.getOrigen(), ruta.getDestino());

        ruta.setDuracionMinutos(routeData.duracionMinutos());
        ruta.setDistanciaKm(routeData.distanciaKm());
        ruta.setPolylineCasetas(routeData.polylineCasetas());
        ruta.setPolylineLibre(routeData.polylineLibre());
        ruta.setPolylineCasetasIda(routeData.polylineCasetasIda());
        ruta.setPolylineCasetasRegreso(routeData.polylineCasetasRegreso());
        ruta.setPolylineLibreIda(routeData.polylineLibreIda());
        ruta.setPolylineLibreRegreso(routeData.polylineLibreRegreso());

        ruta.getCasetas().clear();
        if (ruta.getUsaCasetas()) {
            int orden = 1;
            for (GoogleMapsService.CasetaToll toll : routeData.casetas()) {
                ruta.getCasetas().add(Caseta.builder()
                        .ruta(ruta)
                        .nombre(toll.nombre())
                        .costo(toll.costo() == null ? 0.0 : toll.costo())
                        .orden(orden++)
                        .latitud(toll.latitud())
                        .longitud(toll.longitud())
                        .build());
            }
        }

        return toResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public RutaResponse actualizarTrazo(Integer id, RutaTrazoRequest request) {
        Ruta ruta = getRuta(id);
        if (!"CASETA".equalsIgnoreCase(request.tipo()) && !"LIBRE".equalsIgnoreCase(request.tipo())) {
            throw new InvalidEntryException("El tipo de trazo debe ser CASETA o LIBRE");
        }
        if ("CASETA".equalsIgnoreCase(request.tipo())) {
            ruta.setPolylineCasetas(request.polyline());
            ruta.setPolylineCasetasIda(request.polylineIda() == null ? request.polyline() : request.polylineIda());
            ruta.setPolylineCasetasRegreso(request.polylineRegreso());
        } else {
            ruta.setPolylineLibre(request.polyline());
            ruta.setPolylineLibreIda(request.polylineIda() == null ? request.polyline() : request.polylineIda());
            ruta.setPolylineLibreRegreso(request.polylineRegreso());
        }
        ruta.setDistanciaKm(request.distanciaKm());
        ruta.setDuracionMinutos(request.duracionMinutos());
        return toResponse(rutaRepository.save(ruta));
    }

    @Transactional
    public void cambiarEstado(Integer id, Boolean activo) {
        if (activo == null) {
            throw new InvalidEntryException("El estado activo es obligatorio");
        }
        Ruta ruta = getRuta(id);
        ruta.setActivo(activo);
        rutaRepository.save(ruta);
    }

    public Ruta getRuta(Integer id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ruta no encontrada con id " + id));
    }

    public double costoTotalCasetas(Ruta ruta) {
        double suma = ruta.getCasetas().stream()
                .mapToDouble(Caseta::getCosto)
                .sum();
        return Math.round(suma * 100.0) / 100.0;
    }

    private RutaResponse toResponse(Ruta ruta) {
        List<CasetaResponse> casetas = ruta.getCasetas().stream()
                .sorted(Comparator.comparing(Caseta::getOrden))
                .map(c -> new CasetaResponse(
                        c.getIdCaseta(), c.getNombre(), c.getCosto(),
                        c.getOrden(), c.getLatitud(), c.getLongitud()))
                .toList();
        return new RutaResponse(
                ruta.getIdRuta(),
                ruta.getNombre(),
                toDestinoResponse(ruta.getOrigen()),
                toDestinoResponse(ruta.getDestino()),
                ruta.getUsaCasetas(),
                ruta.getDuracionMinutos(),
                ruta.getDistanciaKm(),
                ruta.getCostoComida(),
                ruta.getCostoHospedaje(),
                costoTotalCasetas(ruta),
                 ruta.getPolylineCasetas(),
                 ruta.getPolylineLibre(),
                 ruta.getPolylineCasetasIda(),
                 ruta.getPolylineCasetasRegreso(),
                 ruta.getPolylineLibreIda(),
                 ruta.getPolylineLibreRegreso(),
                 casetas,
                ruta.getActivo()
        );
    }

    private DestinoResponse toDestinoResponse(Destino d) {
        return new DestinoResponse(
                d.getIdDestino(), d.getNombre(), d.getDireccion(),
                d.getLatitud(), d.getLongitud(), d.getActivo());
    }

    private void validarDestinos(Integer idOrigen, Integer idDestino) {
        if (idOrigen.equals(idDestino)) {
            throw new InvalidEntryException("El origen y el destino deben ser diferentes");
        }
    }

    private void validarDestinosActivos(Destino origen, Destino destino) {
        if (!origen.getActivo() || !destino.getActivo()) {
            throw new InvalidEntryException("El origen y el destino deben estar activos");
        }
    }
}
