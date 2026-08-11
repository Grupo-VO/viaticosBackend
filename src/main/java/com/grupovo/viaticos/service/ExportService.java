package com.grupovo.viaticos.service;

import com.grupovo.viaticos.domain.CargaCombustible;
import com.grupovo.viaticos.domain.Mantenimiento;
import com.grupovo.viaticos.domain.Ruta;
import com.grupovo.viaticos.domain.Vehiculo;
import com.grupovo.viaticos.domain.Viaje;
import com.grupovo.viaticos.domain.en.TipoTrayecto;
import com.grupovo.viaticos.repository.CargaCombustibleRepository;
import com.grupovo.viaticos.repository.MantenimientoRepository;
import com.grupovo.viaticos.repository.ViajeRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ViajeRepository viajeRepository;
    private final CargaCombustibleRepository cargaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final RutaService rutaService;
    private final ConfiguracionService configuracionService;

    public ExportService(ViajeRepository viajeRepository,
                         CargaCombustibleRepository cargaRepository,
                         MantenimientoRepository mantenimientoRepository,
                         RutaService rutaService,
                         ConfiguracionService configuracionService) {
        this.viajeRepository = viajeRepository;
        this.cargaRepository = cargaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.rutaService = rutaService;
        this.configuracionService = configuracionService;
    }

    @Transactional(readOnly = true)
    public byte[] viajesExcel(LocalDate desde, LocalDate hasta) {
        List<Viaje> viajes = viajesEnRango(desde, hasta);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Viajes");
            String[] headers = {"Id", "Ruta", "Origen", "Destino", "Vehiculo", "Chofer",
                    "Pasajeros", "Fecha salida", "Fecha regreso", "Tipo trayecto", "Estado"};
            escribirEncabezado(wb, sheet, headers);
            int r = 1;
            for (Viaje v : viajes) {
                Row row = sheet.createRow(r++);
                celda(row, 0, v.getIdViaje());
                celda(row, 1, v.getRuta().getNombre());
                celda(row, 2, v.getRuta().getOrigen().getNombre());
                celda(row, 3, v.getRuta().getDestino().getNombre());
                celda(row, 4, v.getVehiculo().getPlaca());
                celda(row, 5, v.getChofer().getNombre() + " " + v.getChofer().getApellidoPaterno());
                celda(row, 6, v.getNumeroPasajeros());
                celda(row, 7, v.getFechaSalida().format(FECHA_HORA));
                celda(row, 8, v.getFechaRegreso().format(FECHA_HORA));
                celda(row, 9, v.getTipoTrayecto().name());
                celda(row, 10, v.getStatus().name());
            }
            ajustarColumnas(sheet, headers.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el reporte de viajes en Excel", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] viajesPdf(LocalDate desde, LocalDate hasta) {
        List<Viaje> viajes = viajesEnRango(desde, hasta);
        return pdf("Reporte de viajes", "Periodo: " + periodo(desde, hasta),
                new String[]{"Id", "Ruta", "Vehiculo", "Chofer", "Salida", "Regreso", "Estado"},
                viajes, (v) -> new String[]{
                        String.valueOf(v.getIdViaje()),
                        v.getRuta().getNombre(),
                        v.getVehiculo().getPlaca(),
                        v.getChofer().getNombre() + " " + v.getChofer().getApellidoPaterno(),
                        v.getFechaSalida().format(FECHA_HORA),
                        v.getFechaRegreso().format(FECHA_HORA),
                        v.getStatus().name()});
    }

    @Transactional(readOnly = true)
    public byte[] viaticosExcel(LocalDate desde, LocalDate hasta) {
        List<Viaje> viajes = viajesEnRango(desde, hasta);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Viaticos");
            String[] headers = {"Id", "Ruta", "Vehiculo", "Chofer", "Dias", "Total casetas",
                    "Total viaticos", "Litros", "Costo combustible", "Total viaje"};
            escribirEncabezado(wb, sheet, headers);
            int r = 1;
            double sumaCasetas = 0, sumaViaticos = 0, sumaCombustible = 0, sumaViaje = 0;
            for (Viaje v : viajes) {
                FilaViaje f = filaViaje(v);
                Row row = sheet.createRow(r++);
                celda(row, 0, v.getIdViaje());
                celda(row, 1, v.getRuta().getNombre());
                celda(row, 2, v.getVehiculo().getPlaca());
                celda(row, 3, v.getChofer().getNombre() + " " + v.getChofer().getApellidoPaterno());
                celda(row, 4, f.totalDias());
                celda(row, 5, f.totalCasetas());
                celda(row, 6, f.totalViaticos());
                celda(row, 7, f.litros());
                celda(row, 8, f.costoCombustible());
                celda(row, 9, f.totalViaje());
                sumaCasetas += f.totalCasetas();
                sumaViaticos += f.totalViaticos();
                sumaCombustible += f.costoCombustible();
                sumaViaje += f.totalViaje();
            }
            Row total = sheet.createRow(r);
            celda(total, 1, "TOTALES");
            celda(total, 5, Math.round(sumaCasetas * 100.0) / 100.0);
            celda(total, 6, Math.round(sumaViaticos * 100.0) / 100.0);
            celda(total, 8, Math.round(sumaCombustible * 100.0) / 100.0);
            celda(total, 9, Math.round(sumaViaje * 100.0) / 100.0);
            ajustarColumnas(sheet, headers.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el reporte de viaticos en Excel", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] viaticosPdf(LocalDate desde, LocalDate hasta) {
        List<Viaje> viajes = viajesEnRango(desde, hasta);
        return pdf("Reporte de viaticos", "Periodo: " + periodo(desde, hasta),
                new String[]{"Id", "Ruta", "Dias", "Casetas", "Viaticos", "Combustible", "Total"},
                viajes, (v) -> {
                    FilaViaje f = filaViaje(v);
                    return new String[]{String.valueOf(v.getIdViaje()), v.getRuta().getNombre(),
                            String.valueOf(f.totalDias()), String.valueOf(f.totalCasetas()),
                            String.valueOf(f.totalViaticos()), String.valueOf(f.costoCombustible()),
                            String.valueOf(f.totalViaje())};
                });
    }

    @Transactional(readOnly = true)
    public byte[] combustibleExcel(LocalDate desde, LocalDate hasta) {
        List<CargaCombustible> cargas = cargasEnRango(desde, hasta);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Combustible");
            String[] headers = {"Id", "Vehiculo", "Fecha", "Litros", "Costo total", "Odometro"};
            escribirEncabezado(wb, sheet, headers);
            int r = 1;
            double sumaLitros = 0, sumaCosto = 0;
            for (CargaCombustible c : cargas) {
                Row row = sheet.createRow(r++);
                celda(row, 0, c.getIdCarga());
                celda(row, 1, c.getVehiculo().getPlaca());
                celda(row, 2, c.getFecha().format(FECHA_HORA));
                celda(row, 3, c.getLitros());
                celda(row, 4, c.getCostoTotal());
                celda(row, 5, c.getOdometro());
                sumaLitros += c.getLitros();
                sumaCosto += c.getCostoTotal();
            }
            Row total = sheet.createRow(r);
            celda(total, 1, "TOTALES");
            celda(total, 3, Math.round(sumaLitros * 100.0) / 100.0);
            celda(total, 4, Math.round(sumaCosto * 100.0) / 100.0);
            ajustarColumnas(sheet, headers.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el reporte de combustible en Excel", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] combustiblePdf(LocalDate desde, LocalDate hasta) {
        List<CargaCombustible> cargas = cargasEnRango(desde, hasta);
        return pdf("Reporte de gasolina", "Periodo: " + periodo(desde, hasta),
                new String[]{"Id", "Vehiculo", "Fecha", "Litros", "Costo", "Odometro"},
                cargas, (c) -> new String[]{String.valueOf(c.getIdCarga()),
                        c.getVehiculo().getPlaca(), c.getFecha().format(FECHA_HORA),
                        String.valueOf(c.getLitros()), String.valueOf(c.getCostoTotal()),
                        String.valueOf(c.getOdometro())});
    }

    @Transactional(readOnly = true)
    public byte[] mantenimientosExcel(LocalDate desde, LocalDate hasta) {
        List<Mantenimiento> mantenimientos = mantenimientosEnRango(desde, hasta);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Mantenimientos");
            String[] headers = {"Id", "Vehiculo", "Tipo", "Estado", "Fecha", "Descripcion",
                    "Costo", "Odometro"};
            escribirEncabezado(wb, sheet, headers);
            int r = 1;
            double sumaCosto = 0;
            for (Mantenimiento m : mantenimientos) {
                Row row = sheet.createRow(r++);
                celda(row, 0, m.getIdMantenimiento());
                celda(row, 1, m.getVehiculo().getPlaca());
                celda(row, 2, m.getTipo().name());
                celda(row, 3, m.getEstado().name());
                celda(row, 4, m.getFecha().format(FECHA_HORA));
                celda(row, 5, m.getDescripcion());
                celda(row, 6, m.getCosto());
                celda(row, 7, m.getOdometro());
                sumaCosto += m.getCosto();
            }
            Row total = sheet.createRow(r);
            celda(total, 1, "TOTALES");
            celda(total, 6, Math.round(sumaCosto * 100.0) / 100.0);
            ajustarColumnas(sheet, headers.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el reporte de mantenimientos en Excel", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] mantenimientosPdf(LocalDate desde, LocalDate hasta) {
        List<Mantenimiento> mantenimientos = mantenimientosEnRango(desde, hasta);
        return pdf("Reporte de mantenimientos", "Periodo: " + periodo(desde, hasta),
                new String[]{"Id", "Vehiculo", "Tipo", "Estado", "Fecha", "Costo"},
                mantenimientos, (m) -> new String[]{String.valueOf(m.getIdMantenimiento()),
                        m.getVehiculo().getPlaca(), m.getTipo().name(), m.getEstado().name(),
                        m.getFecha().format(FECHA_HORA), String.valueOf(m.getCosto())});
    }

    private List<Viaje> viajesEnRango(LocalDate desde, LocalDate hasta) {
        return viajeRepository.findAllByFechaSalidaBetweenOrderByFechaSalidaDesc(inicio(desde), fin(hasta));
    }

    private List<CargaCombustible> cargasEnRango(LocalDate desde, LocalDate hasta) {
        return cargaRepository.findAllByFechaBetweenOrderByFechaDesc(inicio(desde), fin(hasta));
    }

    private List<Mantenimiento> mantenimientosEnRango(LocalDate desde, LocalDate hasta) {
        return mantenimientoRepository.findAllByFechaBetweenOrderByFechaDesc(inicio(desde), fin(hasta));
    }

    private LocalDateTime inicio(LocalDate desde) {
        return (desde != null ? desde : LocalDate.of(2000, 1, 1)).atStartOfDay();
    }

    private LocalDateTime fin(LocalDate hasta) {
        return (hasta != null ? hasta : LocalDate.now().plusYears(10)).atTime(LocalTime.MAX);
    }

    private String periodo(LocalDate desde, LocalDate hasta) {
        return (desde == null ? "inicio" : desde.format(FECHA)) + " a " + (hasta == null ? "hoy" : hasta.format(FECHA));
    }

    private FilaViaje filaViaje(Viaje viaje) {
        Ruta ruta = viaje.getRuta();
        Vehiculo vehiculo = viaje.getVehiculo();
        int totalDias = (int) ChronoUnit.DAYS.between(
                viaje.getFechaSalida().toLocalDate(), viaje.getFechaRegreso().toLocalDate()) + 1;
        double totalViaticos = (ruta.getCostoComida() + ruta.getCostoHospedaje()) * totalDias;
        double totalCasetas = viaje.getTipoTrayecto() == TipoTrayecto.CASETAS
                ? rutaService.costoTotalCasetas(ruta) : 0.0;
        double litros = Math.round(ruta.getDistanciaKm() / vehiculo.getRendimiento() * 100.0) / 100.0;
        double precio = configuracionService.getDouble(ConfiguracionService.PRECIO_COMBUSTIBLE, 0.0);
        double costoCombustible = Math.round(litros * precio * 100.0) / 100.0;
        return new FilaViaje(totalDias, Math.round(totalCasetas * 100.0) / 100.0,
                Math.round(totalViaticos * 100.0) / 100.0, litros, costoCombustible,
                Math.round((totalViaticos + totalCasetas) * 100.0) / 100.0);
    }

    private void escribirEncabezado(Workbook wb, Sheet sheet, String[] headers) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void ajustarColumnas(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void celda(Row row, int index, Object valor) {
        Cell cell = row.createCell(index);
        if (valor == null) {
            return;
        }
        if (valor instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(valor.toString());
        }
    }

    @FunctionalInterface
    private interface FilaAExcel<T> {
        String[] aExcel(T item);
    }

    private <T> byte[] pdf(String titulo, String subtitulo, String[] headers, List<T> datos, FilaAExcel<T> fila) {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font celdaFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph p = new Paragraph(titulo, tituloFont);
            p.setAlignment(Element.ALIGN_CENTER);
            document.add(p);
            Paragraph sp = new Paragraph(subtitulo, subtituloFont);
            sp.setAlignment(Element.ALIGN_CENTER);
            document.add(sp);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            for (T item : datos) {
                for (String valor : fila.aExcel(item)) {
                    PdfPCell cell = new PdfPCell(new Phrase(valor, celdaFont));
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el reporte en PDF", e);
        }
    }

    private record FilaViaje(int totalDias, double totalCasetas, double totalViaticos,
                             double litros, double costoCombustible, double totalViaje) {
    }
}
