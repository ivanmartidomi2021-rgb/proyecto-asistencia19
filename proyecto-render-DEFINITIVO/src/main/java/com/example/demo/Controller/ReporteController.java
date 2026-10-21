package com.example.demo.Controller;

import com.example.demo.model.Alumno;
import com.example.demo.model.Asistencia;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.AsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/reportes")
    public String verVistaPrevia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Integer idAlumno,
            @RequestParam(required = false) String estado,
            Model model) {

        // 1. Fechas por defecto (mes actual)
        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fin == null) fin = LocalDate.now();

        // 2. Obtener y filtrar datos
        List<Asistencia> asistenciasRango = asistenciaRepository.findByFechaBetween(inicio, fin);
        List<Alumno> listaAlumnos = alumnoRepository.findAll();

        List<Asistencia> filtradas = asistenciasRango.stream()
                .filter(a -> idAlumno == null || a.getIdAlumno().equals(idAlumno))
                .filter(a -> estado == null || estado.trim().isEmpty() || a.getEstado().equalsIgnoreCase(estado))
                .collect(Collectors.toList());

        // 3. Crear mapa de Alumnos para el HTML
        Map<Integer, String> mapaAlumnos = new HashMap<>();
        for (Alumno a : listaAlumnos) {
            mapaAlumnos.put(a.getIdAlumno(), a.getNombres() + " " + a.getApellidos());
        }

        // 4. Calcular Estadísticas
        long presentes = filtradas.stream().filter(a -> "Presente".equals(a.getEstado())).count();
        long ausentes = filtradas.stream().filter(a -> "Ausente".equals(a.getEstado())).count();
        long total = filtradas.size();
        double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;

        // 5. Enviar a la vista
        model.addAttribute("inicio", inicio);
        model.addAttribute("fin", fin);
        model.addAttribute("idAlumnoSeleccionado", idAlumno);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("listaAlumnos", listaAlumnos);
        model.addAttribute("asistencias", filtradas);
        model.addAttribute("mapaAlumnos", mapaAlumnos);
        model.addAttribute("totalRegistros", total);
        model.addAttribute("presentes", presentes);
        model.addAttribute("ausentes", ausentes);
        model.addAttribute("porcentaje", Math.round(porcentaje * 100) / 100.0);

        return "reportes";
    }
}
