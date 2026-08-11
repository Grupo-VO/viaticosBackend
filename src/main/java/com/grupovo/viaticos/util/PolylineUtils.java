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
