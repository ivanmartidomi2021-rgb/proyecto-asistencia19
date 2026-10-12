package com.example.demo.Controller;

import com.example.demo.model.Asistencia;
import com.example.demo.model.Alumno;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.AlumnoRepository;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class AsistenciaController {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AlumnoRepository alumnoRepository;

    // ============================================================
    // 📋 MOSTRAR LISTA DE ASISTENCIAS CON FILTRO POR FECHA
    // ============================================================
    @GetMapping("/asistencias")
    public String asistencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {

        if (fecha == null) {
            fecha = LocalDate.now();
        }

        List<Alumno> alumnos = alumnoRepository.findAll();
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);

        Map<Integer, String> asistenciaMap = new HashMap<>();
        Map<Integer, String> bitacoraMap = new HashMap<>();

        for (Asistencia a : asistencias) {
            asistenciaMap.put(a.getIdAlumno(), a.getEstado());
            if (a.getBitacora() != null && !a.getBitacora().isEmpty()) {
                bitacoraMap.put(a.getIdAlumno(), a.getBitacora());
            }
        }

        model.addAttribute("alumnos", alumnos);
        model.addAttribute("asistencias", asistencias);
        model.addAttribute("asistenciaMap", asistenciaMap);
        model.addAttribute("bitacoraMap", bitacoraMap);
        model.addAttribute("fechaSeleccionada", fecha);
        model.addAttribute("totalAlumnos", alumnos.size());

        long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();

        model.addAttribute("presentes", presentes);
        model.addAttribute("ausentes", ausentes);

        return "asistencias";
    }

    // ============================================================
    // 💾 GUARDAR ASISTENCIAS MASIVAS
    // ============================================================
    @PostMapping("/guardarAsistencia")
    public String guardarAsistencias(
            @RequestParam String fecha,
            @RequestParam(required = false) List<Integer> alumnosPresentes,
            @RequestParam Map<String, String> allParams) {

        LocalDate fechaAsistencia = LocalDate.parse(fecha);
        List<Alumno> todosLosAlumnos = alumnoRepository.findAll();

        List<Asistencia> existentes = asistenciaRepository.findByFecha(fechaAsistencia);
        if (!existentes.isEmpty()) {
            asistenciaRepository.deleteAll(existentes);
        }

        String bitacoraGeneral = allParams.getOrDefault("bitacora", "");

        for (Alumno alumno : todosLosAlumnos) {
            Asistencia asistencia = new Asistencia();
            asistencia.setIdAlumno(alumno.getIdAlumno());
            asistencia.setFecha(fechaAsistencia);

            String bitacoraIndividual = allParams.get("bitacora_" + alumno.getIdAlumno());
            if (bitacoraIndividual != null && !bitacoraIndividual.isEmpty()) {
                asistencia.setBitacora(bitacoraIndividual);
            } else {
                asistencia.setBitacora(bitacoraGeneral);
            }

            if (alumnosPresentes != null && alumnosPresentes.contains(alumno.getIdAlumno())) {
                asistencia.setEstado("Presente");
            } else {
                asistencia.setEstado("Ausente");
            }

            asistenciaRepository.save(asistencia);
        }

        return "redirect:/asistencias?fecha=" + fecha;
    }

    // ============================================================
    // 📊 OBTENER ESTADÍSTICAS (JSON)
    // ============================================================
    @GetMapping("/asistencias/estadisticas")
    @ResponseBody
    public Map<String, Object> getEstadisticas(@RequestParam String fecha) {
        LocalDate fechaAsistencia = LocalDate.parse(fecha);
        List<Alumno> alumnos = alumnoRepository.findAll();
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fechaAsistencia);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", alumnos.size());

        long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();

        stats.put("presentes", presentes);
        stats.put("ausentes", ausentes);

        return stats;
    }

    // ============================================================
    // 📋 VER ASISTENCIA DE UN ALUMNO ESPECÍFICO
    // ============================================================
    @GetMapping("/asistencias/alumno/{id}")
    public String verAsistenciaAlumno(@PathVariable Integer id, Model model) {
        Alumno alumno = alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado: " + id));

        List<Asistencia> asistencias = asistenciaRepository.findByIdAlumno(id);

        long total = asistencias.size();
        long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = total - presentes;
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        model.addAttribute("alumno", alumno);
        model.addAttribute("asistencias", asistencias);
        model.addAttribute("total", total);
        model.addAttribute("presentes", presentes);
        model.addAttribute("ausentes", ausentes);
        model.addAttribute("porcentaje", Math.round(porcentaje * 100) / 100.0);

        return "asistencia-alumno";
    }

    // ============================================================
    // 📄 PÁGINA DE REPORTES
    // ============================================================
    @GetMapping("/reportes")
    public String verReportes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            Model model) {

        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        List<Asistencia> asistencias = asistenciaRepository.findByFechaBetween(inicio, fin);
        List<Alumno> alumnos = alumnoRepository.findAll();

        long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes  = asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
        long total     = asistencias.size();
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        model.addAttribute("inicio", inicio);
        model.addAttribute("fin", fin);
        model.addAttribute("totalAlumnos", alumnos.size());
        model.addAttribute("totalRegistros", total);
        model.addAttribute("presentes", presentes);
        model.addAttribute("ausentes", ausentes);
        model.addAttribute("porcentaje", Math.round(porcentaje * 100) / 100.0);

        return "reportes";
    }

    // ============================================================
    // 📄 EXPORTAR REPORTE A PDF (iText 7)
    // ============================================================
    @GetMapping("/reportes/pdf")
    public void exportarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            HttpServletResponse response) throws IOException {

        List<Asistencia> asistencias = asistenciaRepository.findByFechaBetween(inicio, fin);
        List<Alumno> alumnos = alumnoRepository.findAll();

        Map<Integer, String> nombreAlumno = new HashMap<>();
        for (Alumno a : alumnos) {
            nombreAlumno.put(a.getIdAlumno(), a.getNombre() + " " + a.getApellido());
        }

        // ---- Configurar respuesta HTTP ----
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=reporte_asistencias_" + inicio + "_a_" + fin + ".pdf");

        // ---- Crear documento iText 7 ----
        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(40, 40, 40, 40);

        // Fuentes
        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ---- Título ----
        Paragraph titulo = new Paragraph("Reporte de Asistencias")
                .setFont(bold)
                .setFontSize(20)
                .setFontColor(new DeviceRgb(102, 126, 234))
                .setTextAlignment(TextAlignment.CENTER);
        document.add(titulo);

        // ---- Subtítulo rango ----
        Paragraph subtitulo = new Paragraph("Periodo: " + inicio + "  al  " + fin)
                .setFont(normal)
                .setFontSize(11)
                .setFontColor(new DeviceRgb(74, 74, 106))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(subtitulo);

        // ---- Resumen ----
        long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes  = asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
        long total     = asistencias.size();
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        document.add(new Paragraph("Resumen general:").setFont(bold).setFontSize(12));
        document.add(new Paragraph("• Total de registros: " + total).setFont(normal).setFontSize(10));
        document.add(new Paragraph("• Presentes: " + presentes).setFont(normal).setFontSize(10));
        document.add(new Paragraph("• Ausentes: " + ausentes).setFont(normal).setFontSize(10));
        document.add(new Paragraph("• Porcentaje de asistencia: "
                + (Math.round(porcentaje * 100) / 100.0) + " %")
                .setFont(normal).setFontSize(10)
                .setMarginBottom(15));

        // ---- Tabla de detalle ----
        float[] columnWidths = {2f, 3f, 2f, 4f};
        Table tabla = new Table(UnitValue.createPercentArray(columnWidths));
        tabla.setWidth(UnitValue.createPercentValue(100));

        // Cabeceras
        String[] headers = {"Fecha", "Alumno", "Estado", "Bitácora"};
        DeviceRgb headerBg = new DeviceRgb(102, 126, 234);
        for (String h : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(10)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(headerBg)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(6);
            tabla.addHeaderCell(cell);
        }

        // Filas
        DeviceRgb verde = new DeviceRgb(212, 237, 218);
        DeviceRgb rojo  = new DeviceRgb(248, 215, 218);

        for (Asistencia a : asistencias) {
            String nombre = nombreAlumno.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno());
            String bitacora = a.getBitacora() == null ? "" : a.getBitacora();

            Cell c1 = new Cell().add(new Paragraph(a.getFecha().toString())
                    .setFont(normal).setFontSize(9)).setPadding(5);

            Cell c2 = new Cell().add(new Paragraph(nombre)
                    .setFont(normal).setFontSize(9)).setPadding(5);

            Cell c3 = new Cell().add(new Paragraph(a.getEstado())
                    .setFont(normal).setFontSize(9)).setPadding(5);

            if ("Presente".equals(a.getEstado())) {
                c3.setBackgroundColor(verde);
            } else {
                c3.setBackgroundColor(rojo);
            }

            Cell c4 = new Cell().add(new Paragraph(bitacora)
                    .setFont(normal).setFontSize(9)).setPadding(5);

            tabla.addCell(c1);
            tabla.addCell(c2);
            tabla.addCell(c3);
            tabla.addCell(c4);
        }

        document.add(tabla);

        // ---- Pie de página ----
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Generado automáticamente por el Sistema Escolar")
                .setFont(normal)
                .setFontSize(8)
                .setFontColor(new DeviceRgb(138, 138, 170))
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
    }
}
