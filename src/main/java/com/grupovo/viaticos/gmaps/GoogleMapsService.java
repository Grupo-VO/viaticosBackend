package com.grupovo.viaticos.gmaps;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.grupovo.viaticos.domain.Destino;
import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.util.PolylineUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GoogleMapsService {

    private static final String BASE_URL = "https://routes.googleapis.com";
    private static final String FIELD_MASK =
            "routes.duration,routes.distanceMeters,routes.polyline,routes.travelAdvisory.tollInfo";

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GoogleMapsService(@Value("${google.maps.api-key:}") String apiKey,
                             ObjectMapper objectMapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = RestClient.create(BASE_URL);
        this.objectMapper = objectMapper;
    }

    public RouteData calcularRutaRedonda(Destino origen, Destino destino) {
        if (apiKey.isEmpty()) {
            return mockRouteData();
        }

        RouteResult porCasetas = computeRoute(origen, destino, false);
        RouteResult libre = computeRoute(origen, destino, true);

        List<CasetaToll> casetas = porCasetas.tolles();

        return new RouteData(
                round(porCasetas.durationSeconds() / 60.0 * 2),
                round(porCasetas.distanceMeters() / 1000.0 * 2),
                PolylineUtils.roundTrip(porCasetas.encodedPolyline()),
                PolylineUtils.roundTrip(libre.encodedPolyline()),
                casetas
        );
    }

    private RouteResult computeRoute(Destino origen, Destino destino, boolean avoidTolls) {
        Map<String, Object> body = Map.of(
                "origin", Map.of("location", Map.of("latLng", Map.of(
                        "latitude", origen.getLatitud(), "longitude", origen.getLongitud()))),
                "destination", Map.of("location", Map.of("latLng", Map.of(
                        "latitude", destino.getLatitud(), "longitude", destino.getLongitud()))),
                "travelMode", "DRIVE",
                "polylineEncoding", "ENCODED_POLYLINE",
                "units", "METRIC",
                "routeModifiers", Map.of("avoidTolls", avoidTolls)
        );

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new InvalidEntryException("No se pudo calcular la ruta en Google Maps: " + ex.getMessage());
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode route = root.path("routes").path(0);
            if (route.isMissingNode()) {
                throw new InvalidEntryException("Google Maps no encontró una ruta entre los destinos");
            }
            String polyline = route.path("polyline").path("encodedPolyline").asText(null);
            long durationSeconds = parseSeconds(route.path("duration").asText("0s"));
            long distanceMeters = route.path("distanceMeters").asLong(0);
            List<CasetaToll> tolles = new ArrayList<>();
            if (!avoidTolls) {
                JsonNode tollInfo = route.path("travelAdvisory").path("tollInfo");
                JsonNode tolls = tollInfo.path("tolls");
                int i = 1;
                for (JsonNode toll : tolls) {
                    JsonNode location = toll.path("tollPlaces").path(0).path("location").path("latLng");
                    Double lat = location.path("latitude").isNumber()
                            ? location.path("latitude").asDouble() : null;
                    Double lng = location.path("longitude").isNumber()
                            ? location.path("longitude").asDouble() : null;
                    Double costo = moneyToDouble(toll.path("estimatedPrice").path(0));
                    tolles.add(new CasetaToll("Caseta " + i, costo, lat, lng));
                    i++;
                }
            }
            return new RouteResult(durationSeconds, distanceMeters, polyline, tolles);
        } catch (InvalidEntryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidEntryException("Error al interpretar la respuesta de Google Maps");
        }
    }

    private long parseSeconds(String duration) {
        try {
            String value = duration == null ? "0s" : duration;
            return Long.parseLong(value.replace("s", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Double moneyToDouble(JsonNode money) {
        if (money == null || money.isMissingNode()) {
            return 0.0;
        }
        long units = money.path("units").asLong(0);
        long nanos = money.path("nanos").asLong(0);
        return Math.round((units + nanos / 1_000_000_000.0) * 100.0) / 100.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private RouteData mockRouteData() {
        List<CasetaToll> casetas = List.of(
                new CasetaToll("Caseta 1 (Demo)", 385.0, null, null),
                new CasetaToll("Caseta 2 (Demo)", 270.0, null, null)
        );
        String mockPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        return new RouteData(
                240.0,
                320.0,
                PolylineUtils.roundTrip(mockPolyline),
                PolylineUtils.roundTrip(mockPolyline),
                casetas
        );
    }

    public record CasetaToll(String nombre, Double costo, Double latitud, Double longitud) {
    }

    public record RouteData(Double duracionMinutos,
                            Double distanciaKm,
                            String polylineCasetas,
                            String polylineLibre,
                            List<CasetaToll> casetas) {
    }

    private record RouteResult(long durationSeconds, long distanceMeters,
                               String encodedPolyline, List<CasetaToll> tolles) {
    }
}
