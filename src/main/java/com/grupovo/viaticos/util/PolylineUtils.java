package com.grupovo.viaticos.util;

import java.util.ArrayList;
import java.util.List;

public final class PolylineUtils {

    private PolylineUtils() {
    }

    public static List<double[]> decode(String encoded) {
        List<double[]> points = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        while (index < len) {
            int result = 0;
            int shift = 0;
            int b;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            result = 0;
            shift = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            points.add(new double[]{lat / 1e5, lng / 1e5});
        }
        return points;
    }

    public static String encode(List<double[]> points) {
        StringBuilder sb = new StringBuilder();
        long prevLat = 0;
        long prevLng = 0;
        for (double[] point : points) {
            long lat = Math.round(point[0] * 1e5);
            long lng = Math.round(point[1] * 1e5);
            encodeValue(sb, lat - prevLat);
            encodeValue(sb, lng - prevLng);
            prevLat = lat;
            prevLng = lng;
        }
        return sb.toString();
    }

    public static String concat(String primera, String segunda) {
        if (primera == null || primera.isEmpty()) {
            return segunda;
        }
        if (segunda == null || segunda.isEmpty()) {
            return primera;
        }
        List<double[]> puntos = new ArrayList<>(decode(primera));
        List<double[]> regreso = decode(segunda);
        double[] ultimo = puntos.get(puntos.size() - 1);
        double[] primeroRegreso = regreso.get(0);
        if (distanciaMetros(ultimo[0], ultimo[1], primeroRegreso[0], primeroRegreso[1]) > 1.0) {
            puntos.add(primeroRegreso);
        }
        for (int i = 1; i < regreso.size(); i++) {
            puntos.add(regreso.get(i));
        }
        return encode(puntos);
    }

    public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    public static double distanciaPuntoASegmento(double lat, double lng,
                                                 double lat1, double lng1,
                                                 double lat2, double lng2) {
        double dx = lng2 - lng1;
        double dy = lat2 - lat1;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0 : ((lng - lng1) * dx + (lat - lat1) * dy) / lenSq;
        t = Math.max(0.0, Math.min(1.0, t));
        double latProy = lat1 + t * dy;
        double lngProy = lng1 + t * dx;
        return distanciaMetros(lat, lng, latProy, lngProy);
    }

    public static String roundTrip(String oneWay) {
        if (oneWay == null || oneWay.isEmpty()) {
            return oneWay;
        }
        List<double[]> points = new ArrayList<>(decode(oneWay));
        if (points.size() <= 1) {
            return oneWay;
        }
        for (int i = points.size() - 2; i >= 1; i--) {
            points.add(points.get(i));
        }
        return encode(points);
    }

    private static void encodeValue(StringBuilder sb, long value) {
        long v = value << 1;
        if (value < 0) {
            v = ~v;
        }
        while (v >= 0x20) {
            sb.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        sb.append((char) (v + 63));
    }
}
