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
import org.springframework.web.bind.annotation.ResponseBody;

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

    // 1. VISTA PRINCIPAL DE REPORTES
    @GetMapping("/reportes")
    public String verReportes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAlumno,
            @RequestParam(required = false) String estado,
            Model model) {

        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        model.addAttribute("inicio", inicio);
        model.addAttribute("fin", fin);
        model.addAttribute("idAlumnoSeleccionado", idAlumno);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("listaAlumnos", alumnoRepository.findAll());

        return "reportes";
    }

    // 2. API JSON PARA ACTUALIZACIÓN DINÁMICA (AJAX)
    @GetMapping("/reportes/datos")
    @ResponseBody
    public Map<String, Object> obtenerDatosReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAlumno,
            @RequestParam(required = false) String estado) {

        List<Asistencia> todas = asistenciaRepository.findByFechaBetween(inicio, fin);
        List<Alumno> alumnos = alumnoRepository.findAll();

        Map<Integer, String> mapaAlumnos = new HashMap<>();
        for (Alumno a : alumnos) {
            mapaAlumnos.put(a.getIdAlumno(), a.getNombres() + " " + a.getApellidos());
        }

        List<Asistencia> filtradas = todas.stream()
                .filter(a -> idAlumno == null || a.getIdAlumno().equals(idAlumno))
                .filter(a -> estado == null || estado.trim().isEmpty() || a.getEstado().equalsIgnoreCase(estado))
                .collect(Collectors.toList());

        long presentes = filtradas.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = filtradas.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
        long total = filtradas.size();
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("asistencias", filtradas.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("fecha", a.getFecha().format(DATE_FORMAT));
            map.put("alumno", mapaAlumnos.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno()));
            map.put("estado", a.getEstado());
            map.put("bitacora", a.getBitacora() != null ? a.getBitacora() : "");
            return map;
        }).collect(Collectors.toList()));
        
        response.put("total", total);
        response.put("presentes", presentes);
        response.put("ausentes", ausentes);
        response.put("porcentaje", Math.round(porcentaje * 100) / 100.0);

        return response;
    }

    // 3. EXPORTAR PDF PROFESIONAL
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
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_Asistencias_Oficial.pdf");

        try (OutputStream os = response.getOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Encabezado Formal
            document.add(new Paragraph("SISTEMA ESCOLAR DE CONTROL DE ASISTENCIA")
                    .setFontSize(10).setFontColor(new DeviceRgb(138, 138, 170))
                    .setTextAlignment(TextAlignment.RIGHT));
                    
            document.add(new Paragraph("REPORTE OFICIAL DE ASISTENCIAS")
                    .setFontSize(20).setBold().setFontColor(new DeviceRgb(26, 32, 44))
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(10));
            
            document.add(new Paragraph("PERIODO EVALUADO: " + inicio.format(DATE_FORMAT) + " AL " + fin.format(DATE_FORMAT))
                    .setFontSize(12).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            // Tabla
            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 4, 2, 5})).useAllAvailableWidth();
            String[] headers = {"FECHA", "ESTUDIANTE", "ESTADO", "OBSERVACIONES"};
            
            for (String h : headers) {
                table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(h).setBold().setFontSize(10).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(new DeviceRgb(44, 82, 130))
                        .setTextAlignment(TextAlignment.CENTER).setPadding(8));
            }

            for (Asistencia a : filtradas) {
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(a.getFecha().format(DATE_FORMAT)).setFontSize(9)).setPadding(5));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(mapaAlumnos.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno())).setFontSize(9)).setPadding(5));
                
                com.itextpdf.layout.element.Cell c3 = new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(a.getEstado()).setFontSize(9).setBold())
                        .setPadding(5).setTextAlignment(TextAlignment.CENTER);
                
                if ("Presente".equals(a.getEstado())) {
                    c3.setFontColor(new DeviceRgb(39, 103, 73));
                } else {
                    c3.setFontColor(new DeviceRgb(155, 44, 44));
                }
                table.addCell(c3);
                
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(a.getBitacora() != null ? a.getBitacora() : "-").setFontSize(9)).setPadding(5));
            }

            document.add(table);
            document.close();
        }
    }

    // 4. EXPORTAR EXCEL PROFESIONAL
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
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_Asistencias_Oficial.xlsx");

        try (Workbook workbook = new XSSFWorkbook(); OutputStream os = response.getOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte Oficial");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE ASISTENCIAS: " + inicio.format(DATE_FORMAT) + " AL " + fin.format(DATE_FORMAT));
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            Row headerRow = sheet.createRow(2);
            String[] headers = {"FECHA", "ALUMNO", "ESTADO", "OBSERVACIONES"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 3;
            for (Asistencia a : filtradas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getFecha().format(DATE_FORMAT));
                row.createCell(1).setCellValue(mapaAlumnos.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno()));
                row.createCell(2).setCellValue(a.getEstado());
                row.createCell(3).setCellValue(a.getBitacora() != null ? a.getBitacora() : "-");
            }

            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 9000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 12000);
            sheet.createFreezePane(0, 3); // Congela los encabezados en Excel

            workbook.write(os);
        }
    }
}
