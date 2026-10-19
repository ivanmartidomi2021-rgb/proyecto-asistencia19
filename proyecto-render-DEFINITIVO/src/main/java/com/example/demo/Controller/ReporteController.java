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
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ReporteController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AlumnoRepository alumnoRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==================== VISTA PREVIA ====================
    @GetMapping("/reportes")
    public String verReportes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAlumno,
            @RequestParam(required = false) String estado,
            Model model) {

        // Fechas por defecto (mes actual)
        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        // 1. Obtener datos
        List<Asistencia> todas = asistenciaRepository.findByFechaBetween(inicio, fin);
        List<Alumno> alumnos = alumnoRepository.findAll();

        // Mapa para buscar nombres rápido en la tabla
        Map<Integer, String> mapaAlumnos = new HashMap<>();
        for (Alumno a : alumnos) {
            mapaAlumnos.put(a.getIdAlumno(), a.getNombres() + " " + a.getApellidos());
        }

        // 2. Filtrar
        List<Asistencia> filtradas = todas.stream()
                .filter(a -> idAlumno == null || a.getIdAlumno().equals(idAlumno))
                .filter(a -> estado == null || estado.trim().isEmpty() || a.getEstado().equalsIgnoreCase(estado))
                .collect(Collectors.toList());

        // 3. Calcular estadísticas
        long presentes = filtradas.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = filtradas.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
        long total = filtradas.size();
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        // 4. Enviar a la vista
        model.addAttribute("inicio", inicio);
        model.addAttribute("fin", fin);
        model.addAttribute("idAlumnoSeleccionado", idAlumno);
        model.addAttribute("estadoSeleccionado", estado);
        
        model.addAttribute("listaAlumnos", alumnos);
        model.addAttribute("mapaAlumnos", mapaAlumnos);
        model.addAttribute("asistencias", filtradas);
        
        model.addAttribute("totalRegistros", total);
        model.addAttribute("presentes", presentes);
        model.addAttribute("ausentes", ausentes);
        model.addAttribute("porcentaje", Math.round(porcentaje * 100) / 100.0);

        return "reportes";
    }

    // ==================== EXPORTAR A PDF ====================
    @GetMapping("/reportes/pdf")
    public void exportarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAlumno,
            @RequestParam(required = false) String estado,
            HttpServletResponse response) throws IOException {

        List<Asistencia> todas = asistenciaRepository.findByFechaBetween(inicio, fin);
        Map<Integer, String> mapaAlumnos = new HashMap<>();
        for (Alumno a : alumnoRepository.findAll()) {
            mapaAlumnos.put(a.getIdAlumno(), a.getNombres() + " " + a.getApellidos());
        }

        List<Asistencia> filtradas = todas.stream()
                .filter(a -> idAlumno == null || a.getIdAlumno().equals(idAlumno))
                .filter(a -> estado == null || estado.trim().isEmpty() || a.getEstado().equalsIgnoreCase(estado))
                .collect(Collectors.toList());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_asistencias.pdf");

        try (OutputStream os = response.getOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("REPORTE DE ASISTENCIAS").setFontSize(18).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(102, 126, 234)));
            
            document.add(new Paragraph("Periodo: " + inicio.format(DATE_FORMAT) + " al " + fin.format(DATE_FORMAT))
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2, 4})).useAllAvailableWidth();
            
            // Cabeceras
            String[] headers = {"Fecha", "Alumno", "Estado", "Bitácora"};
            for (String h : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(new DeviceRgb(102, 126, 234))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            // Datos
            for (Asistencia a : filtradas) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(a.getFecha().format(DATE_FORMAT))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(mapaAlumnos.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno()))));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(a.getEstado()))
                        .setFontColor("Presente".equals(a.getEstado()) ? new DeviceRgb(16, 185, 129) : new DeviceRgb(239, 68, 68)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(a.getBitacora() != null ? a.getBitacora() : "")));
            }

            document.add(table);
            document.close();
        }
    }

    // ==================== EXPORTAR A EXCEL ====================
    @GetMapping("/reportes/excel")
    public void exportarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAlumno,
            @RequestParam(required = false) String estado,
            HttpServletResponse response) throws IOException {

        List<Asistencia> todas = asistenciaRepository.findByFechaBetween(inicio, fin);
        Map<Integer, String> mapaAlumnos = new HashMap<>();
        for (Alumno a : alumnoRepository.findAll()) {
            mapaAlumnos.put(a.getIdAlumno(), a.getNombres() + " " + a.getApellidos());
        }

        List<Asistencia> filtradas = todas.stream()
                .filter(a -> idAlumno == null || a.getIdAlumno().equals(idAlumno))
                .filter(a -> estado == null || estado.trim().isEmpty() || a.getEstado().equalsIgnoreCase(estado))
                .collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_asistencias.xlsx");

        try (Workbook workbook = new XSSFWorkbook(); OutputStream os = response.getOutputStream()) {
            Sheet sheet = workbook.createSheet("Asistencias");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE ASISTENCIAS: " + inicio.format(DATE_FORMAT) + " al " + fin.format(DATE_FORMAT));
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // Cabeceras
            Row headerRow = sheet.createRow(2);
            String[] headers = {"Fecha", "Alumno", "Estado", "Bitácora"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 3;
            for (Asistencia a : filtradas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getFecha().format(DATE_FORMAT));
                row.createCell(1).setCellValue(mapaAlumnos.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno()));
                row.createCell(2).setCellValue(a.getEstado());
                row.createCell(3).setCellValue(a.getBitacora() != null ? a.getBitacora() : "");
            }

            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 8000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 12000);

            workbook.write(os);
        }
    }
}
