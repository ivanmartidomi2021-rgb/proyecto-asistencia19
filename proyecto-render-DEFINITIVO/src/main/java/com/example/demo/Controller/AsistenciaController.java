package com.example.demo.Controller;

import com.example.demo.model.Asistencia;
import com.example.demo.model.Alumno;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.AlumnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // 📋 MOSTRAR LISTA DE ASISTENCIAS CON FILTRO POR FECHA
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

    // 💾 GUARDAR ASISTENCIAS MASIVAS
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

    // 📊 OBTENER ESTADÍSTICAS
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

    // 📋 VER ASISTENCIA DE UN ALUMNO ESPECÍFICO
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
    // 📄 REPORTES
    // ============================================================

    // 📄 Página de reportes
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

    // 📄 Exportar reporte a PDF
    @GetMapping("/reportes/pdf")
    public void exportarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        List<Asistencia> asistencias = asistenciaRepository.findByFechaBetween(inicio, fin);
        List<Alumno> alumnos = alumnoRepository.findAll();

        Map<Integer, String> nombreAlumno = new HashMap<>();
        for (Alumno a : alumnos) {
            nombreAlumno.put(a.getIdAlumno(), a.getNombre() + " " + a.getApellido());
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=reporte_asistencias_" + inicio + "_a_" + fin + ".pdf");

        com.lowagie.text.Document document = new com.lowagie.text.Document(
                com.lowagie.text.PageSize.A4, 40, 40, 50, 40);

        try {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // ---- Título ----
            com.lowagie.text.Font titulo = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 18,
                    com.lowagie.text.Font.BOLD,
                    new java.awt.Color(102, 126, 234));
            com.lowagie.text.Paragraph t = new com.lowagie.text.Paragraph(
                    "Reporte de Asistencias", titulo);
            t.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(t);

            // ---- Subtítulo rango ----
            com.lowagie.text.Font sub = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 11,
                    com.lowagie.text.Font.NORMAL,
                    new java.awt.Color(74, 74, 106));
            com.lowagie.text.Paragraph s = new com.lowagie.text.Paragraph(
                    "Periodo: " + inicio + "  al  " + fin, sub);
            s.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(s);
            document.add(new com.lowagie.text.Paragraph(" "));

            // ---- Resumen ----
            long presentes = asistencias.stream().filter(a -> "Presente".equals(a.getEstado())).count();
            long ausentes  = asistencias.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
            long total     = asistencias.size();
            double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

            com.lowagie.text.Font negrita = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD);
            document.add(new com.lowagie.text.Paragraph("Resumen general:", negrita));
            document.add(new com.lowagie.text.Paragraph("• Total de registros: " + total));
            document.add(new com.lowagie.text.Paragraph("• Presentes: " + presentes));
            document.add(new com.lowagie.text.Paragraph("• Ausentes: " + ausentes));
            document.add(new com.lowagie.text.Paragraph(
                    "• Porcentaje de asistencia: " + (Math.round(porcentaje * 100) / 100.0) + " %"));
            document.add(new com.lowagie.text.Paragraph(" "));

            // ---- Tabla de detalle ----
            com.lowagie.text.pdf.PdfPTable tabla = new com.lowagie.text.pdf.PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{2f, 3f, 2f, 4f});

            String[] headers = {"Fecha", "Alumno", "Estado", "Bitácora"};
            com.lowagie.text.Font headFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10,
                    com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

            for (String h : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(h, headFont));
                cell.setBackgroundColor(new java.awt.Color(102, 126, 234));
                cell.setPadding(6);
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                tabla.addCell(cell);
            }

            com.lowagie.text.Font bodyFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 9);

            for (Asistencia a : asistencias) {
                String nombre = nombreAlumno.getOrDefault(a.getIdAlumno(), "ID " + a.getIdAlumno());
                String bitacora = a.getBitacora() == null ? "" : a.getBitacora();

                com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(a.getFecha().toString(), bodyFont));
                com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(nombre, bodyFont));
                com.lowagie.text.pdf.PdfPCell c3 = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(a.getEstado(), bodyFont));
                com.lowagie.text.pdf.PdfPCell c4 = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(bitacora, bodyFont));

                if ("Presente".equals(a.getEstado())) {
                    c3.setBackgroundColor(new java.awt.Color(212, 237, 218));
                } else {
                    c3.setBackgroundColor(new java.awt.Color(248, 215, 218));
                }

                for (com.lowagie.text.pdf.PdfPCell c : new com.lowagie.text.pdf.PdfPCell[]{c1, c2, c3, c4}) {
                    c.setPadding(5);
                }

                tabla.addCell(c1);
                tabla.addCell(c2);
                tabla.addCell(c3);
                tabla.addCell(c4);
            }

            document.add(tabla);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }
}
