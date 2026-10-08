package com.example.demo.Controller;

import com.example.demo.model.Alumno;
import com.example.demo.model.Asistencia;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.AsistenciaRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReporteController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AlumnoRepository alumnoRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ============================================================
    // 📄 REPORTE PDF - ASISTENCIA DIARIA
    // ============================================================
    @GetMapping("/reportes/asistencia-diaria/pdf")
    public void reporteAsistenciaDiariaPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=asistencia_" + fecha.format(FILE_DATE_FORMAT) + ".pdf");

        List<Alumno> alumnos = alumnoRepository.findAll();
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);

        // Mapear asistencias por idAlumno
        Map<Integer, Asistencia> mapAsistencia = new HashMap<>();
        for (Asistencia a : asistencias) {
            mapAsistencia.put(a.getIdAlumno(), a);
        }

        try (OutputStream os = response.getOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // ===== TÍTULO =====
            Paragraph titulo = new Paragraph("REPORTE DE ASISTENCIA DIARIA")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(102, 126, 234));
            document.add(titulo);

            document.add(new Paragraph("Fecha: " + fecha.format(DATE_FORMAT))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // ===== ESTADÍSTICAS =====
            long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
            long ausentes = asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();

            Paragraph stats = new Paragraph(
                    "Total Alumnos: " + alumnos.size() +
                    "    |    Presentes: " + presentes +
                    "    |    Ausentes: " + ausentes)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(stats);

            // ===== TABLA =====
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 4, 2, 4}))
                    .useAllAvailableWidth();

            // Encabezados
            String[] headers = {"#", "Alumno", "Estado", "Bitácora"};
            for (String h : headers) {
                Cell cell = new Cell()
                        .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(new DeviceRgb(102, 126, 234))
                        .setTextAlignment(TextAlignment.CENTER);
                table.addHeaderCell(cell);
            }

            // Filas
            int index = 1;
            for (Alumno alumno : alumnos) {
                Asistencia asist = mapAsistencia.get(alumno.getIdAlumno());
                String estado = (asist != null) ? asist.getEstado() : "Ausente";
                String bitacora = (asist != null && asist.getBitacora() != null) ? asist.getBitacora() : "";

                table.addCell(new Cell().add(new Paragraph(String.valueOf(index++))));
                table.addCell(new Cell().add(new Paragraph(
                        alumno.getNombres() + " " + alumno.getApellidos())));
                table.addCell(new Cell().add(new Paragraph(estado))
                        .setFontColor("Presente".equals(estado) ?
                                new DeviceRgb(16, 185, 129) : new DeviceRgb(239, 68, 68)));
                table.addCell(new Cell().add(new Paragraph(bitacora)));
            }

            document.add(table);

            // ===== PIE =====
            document.add(new Paragraph("\nGenerado el: " +
                    LocalDate.now().format(DATE_FORMAT))
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(ColorConstants.GRAY));

            document.close();
        }
    }

    // ============================================================
    // 📊 REPORTE EXCEL - ASISTENCIA DIARIA
    // ============================================================
    @GetMapping("/reportes/asistencia-diaria/excel")
    public void reporteAsistenciaDiariaExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=asistencia_" + fecha.format(FILE_DATE_FORMAT) + ".xlsx");

        List<Alumno> alumnos = alumnoRepository.findAll();
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);

        Map<Integer, Asistencia> mapAsistencia = new HashMap<>();
        for (Asistencia a : asistencias) {
            mapAsistencia.put(a.getIdAlumno(), a);
        }

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream os = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("Asistencia " + fecha);

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle presenteStyle = workbook.createCellStyle();
            presenteStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            presenteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle ausenteStyle = workbook.createCellStyle();
            ausenteStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            ausenteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE ASISTENCIA - " + fecha.format(DATE_FORMAT));
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            // Encabezados
            Row headerRow = sheet.createRow(2);
            String[] headers = {"#", "Nombres", "Apellidos", "Estado", "Bitácora"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 3;
            int index = 1;
            for (Alumno alumno : alumnos) {
                Row row = sheet.createRow(rowNum++);
                Asistencia asist = mapAsistencia.get(alumno.getIdAlumno());
                String estado = (asist != null) ? asist.getEstado() : "Ausente";
                String bitacora = (asist != null && asist.getBitacora() != null) ? asist.getBitacora() : "";

                row.createCell(0).setCellValue(index++);
                row.createCell(1).setCellValue(alumno.getNombres());
                row.createCell(2).setCellValue(alumno.getApellidos());

                Cell estadoCell = row.createCell(3);
                estadoCell.setCellValue(estado);
                estadoCell.setCellStyle("Presente".equals(estado) ? presenteStyle : ausenteStyle);

                row.createCell(4).setCellValue(bitacora);
            }

            // Ajustar ancho
            sheet.setColumnWidth(0, 2000);
            sheet.setColumnWidth(1, 6000);
            sheet.setColumnWidth(2, 6000);
            sheet.setColumnWidth(3, 4000);
            sheet.setColumnWidth(4, 10000);

            workbook.write(os);
        }
    }

    // ============================================================
    // 📋 REPORTE CSV - ASISTENCIA DIARIA
    // ============================================================
    @GetMapping("/reportes/asistencia-diaria/csv")
    public void reporteAsistenciaDiariaCSV(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=asistencia_" + fecha.format(FILE_DATE_FORMAT) + ".csv");

        List<Alumno> alumnos = alumnoRepository.findAll();
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);

        Map<Integer, Asistencia> mapAsistencia = new HashMap<>();
        for (Asistencia a : asistencias) {
            mapAsistencia.put(a.getIdAlumno(), a);
        }

        try (OutputStream os = response.getOutputStream()) {
            // BOM para Excel con UTF-8
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);

            StringBuilder sb = new StringBuilder();
            sb.append("REPORTE DE ASISTENCIA - ").append(fecha.format(DATE_FORMAT)).append("\n\n");
            sb.append("#,Nombres,Apellidos,Estado,Bitácora\n");

            int index = 1;
            for (Alumno alumno : alumnos) {
                Asistencia asist = mapAsistencia.get(alumno.getIdAlumno());
                String estado = (asist != null) ? asist.getEstado() : "Ausente";
                String bitacora = (asist != null && asist.getBitacora() != null) ? asist.getBitacora() : "";

                sb.append(index++).append(",")
                  .append(escapeCSV(alumno.getNombres())).append(",")
                  .append(escapeCSV(alumno.getApellidos())).append(",")
                  .append(estado).append(",")
                  .append(escapeCSV(bitacora)).append("\n");
            }

            os.write(sb.toString().getBytes("UTF-8"));
        }
    }

    // ============================================================
    // 📄 REPORTE PDF - HISTORIAL DE ALUMNO
    // ============================================================
    @GetMapping("/reportes/alumno/{id}/pdf")
    public void reporteHistorialAlumnoPDF(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            HttpServletResponse response) throws IOException {

        Alumno alumno = alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + id));

        List<Asistencia> asistencias = asistenciaRepository.findByIdAlumno(id);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=historial_" + alumno.getApellidos() + "_" + id + ".pdf");

        long total = asistencias.size();
        long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = total - presentes;
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        try (OutputStream os = response.getOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Título
            document.add(new Paragraph("HISTORIAL DE ASISTENCIA")
                    .setFontSize(18).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(102, 126, 234)));

            // Datos del alumno
            document.add(new Paragraph("\nAlumno: " + alumno.getNombres() + " " + alumno.getApellidos())
                    .setFontSize(13).setBold());
            document.add(new Paragraph("Grado: " + alumno.getGrado() + " - Sección: " + alumno.getSeccion())
                    .setFontSize(11));

            // Estadísticas
            document.add(new Paragraph("\nRESUMEN ESTADÍSTICO")
                    .setFontSize(12).setBold().setMarginTop(10));

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .useAllAvailableWidth();
            statsTable.addCell(new Cell().add(new Paragraph("Total").setBold()));
            statsTable.addCell(new Cell().add(new Paragraph("Presentes").setBold()));
            statsTable.addCell(new Cell().add(new Paragraph("Ausentes").setBold()));
            statsTable.addCell(new Cell().add(new Paragraph("% Asistencia").setBold()));
            statsTable.addCell(new Cell().add(new Paragraph(String.valueOf(total))));
            statsTable.addCell(new Cell().add(new Paragraph(String.valueOf(presentes))));
            statsTable.addCell(new Cell().add(new Paragraph(String.valueOf(ausentes))));
            statsTable.addCell(new Cell().add(new Paragraph(
                    String.format("%.2f%%", porcentaje))));
            document.add(statsTable);

            // Tabla de asistencias
            document.add(new Paragraph("\nDETALLE DE ASISTENCIAS")
                    .setFontSize(12).setBold().setMarginTop(15));

            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 4}))
                    .useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("Fecha").setBold())
                    .setBackgroundColor(new DeviceRgb(102, 126, 234))
                    .setFontColor(ColorConstants.WHITE));
            table.addHeaderCell(new Cell().add(new Paragraph("Estado").setBold())
                    .setBackgroundColor(new DeviceRgb(102, 126, 234))
                    .setFontColor(ColorConstants.WHITE));
            table.addHeaderCell(new Cell().add(new Paragraph("Bitácora").setBold())
                    .setBackgroundColor(new DeviceRgb(102, 126, 234))
                    .setFontColor(ColorConstants.WHITE));

            for (Asistencia a : asistencias) {
                table.addCell(new Cell().add(new Paragraph(a.getFecha().format(DATE_FORMAT))));
                table.addCell(new Cell().add(new Paragraph(a.getEstado()))
                        .setFontColor("Presente".equals(a.getEstado()) ?
                                new DeviceRgb(16, 185, 129) : new DeviceRgb(239, 68, 68)));
                table.addCell(new Cell().add(new Paragraph(
                        a.getBitacora() != null ? a.getBitacora() : "")));
            }
            document.add(table);

            document.add(new Paragraph("\nGenerado el: " + LocalDate.now().format(DATE_FORMAT))
                    .setFontSize(9).setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(ColorConstants.GRAY));

            document.close();
        }
    }

    // ============================================================
    // 📊 REPORTE EXCEL - HISTORIAL DE ALUMNO
    // ============================================================
    @GetMapping("/reportes/alumno/{id}/excel")
    public void reporteHistorialAlumnoExcel(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            HttpServletResponse response) throws IOException {

        Alumno alumno = alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + id));

        List<Asistencia> asistencias = asistenciaRepository.findByIdAlumno(id);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=historial_" + alumno.getApellidos() + "_" + id + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream os = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("Historial");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("HISTORIAL DE ASISTENCIA - " +
                    alumno.getNombres() + " " + alumno.getApellidos());
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            Row headerRow = sheet.createRow(2);
            String[] headers = {"Fecha", "Estado", "Bitácora"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 3;
            for (Asistencia a : asistencias) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getFecha().format(DATE_FORMAT));
                row.createCell(1).setCellValue(a.getEstado());
                row.createCell(2).setCellValue(a.getBitacora() != null ? a.getBitacora() : "");
            }

            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 12000);

            workbook.write(os);
        }
    }

    // ============================================================
    // 📄 REPORTE PDF - RESUMEN POR RANGO DE FECHAS
    // ============================================================
    @GetMapping("/reportes/rango/pdf")
    public void reporteRangoPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=reporte_rango_" +
                        inicio.format(FILE_DATE_FORMAT) + "_a_" + fin.format(FILE_DATE_FORMAT) + ".pdf");

        List<Alumno> alumnos = alumnoRepository.findAll();

        try (OutputStream os = response.getOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("REPORTE DE ASISTENCIA POR RANGO")
                    .setFontSize(18).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(102, 126, 234)));

            document.add(new Paragraph("Desde: " + inicio.format(DATE_FORMAT) +
                    "  Hasta: " + fin.format(DATE_FORMAT))
                    .setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1, 1.5f}))
                    .useAllAvailableWidth();

            String[] headers = {"Alumno", "Presentes", "Ausentes", "Total", "%"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(new DeviceRgb(102, 126, 234))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            for (Alumno alumno : alumnos) {
                List<Asistencia> asistencias = asistenciaRepository
                        .findByIdAlumnoAndFechaBetween(alumno.getIdAlumno(), inicio, fin);
                long presentes = asistencias.stream()
                        .filter(a -> "Presente".equals(a.getEstado())).count();
                long total = asistencias.size();
                long ausentes = total - presentes;
                double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

                table.addCell(new Cell().add(new Paragraph(
                        alumno.getNombres() + " " + alumno.getApellidos())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(presentes)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(ausentes)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(total)))
                        .setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(
                        String.format("%.2f%%", porcentaje)))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);
            document.add(new Paragraph("\nGenerado el: " + LocalDate.now().format(DATE_FORMAT))
                    .setFontSize(9).setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(ColorConstants.GRAY));

            document.close();
        }
    }

    // ============================================================
    // 🔧 MÉTODO AUXILIAR PARA CSV
    // ============================================================
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
