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

        // Si no hay fecha, usar la actual
        if (fecha == null) {
            fecha = LocalDate.now();
        }

        // Obtener todos los alumnos
        List<Alumno> alumnos = alumnoRepository.findAll();

        // Obtener asistencias de la fecha seleccionada
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fecha);

        // Crear un mapa para fácil acceso: alumnoId -> estado
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
        
        // Calcular presentes y ausentes
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
            @RequestParam(required = false) String bitacora,
            @RequestParam Map<String, String> allParams) {

        LocalDate fechaAsistencia = LocalDate.parse(fecha);
        List<Alumno> todosLosAlumnos = alumnoRepository.findAll();

        // Primero, eliminar asistencias existentes para esta fecha (evitar duplicados)
        List<Asistencia> existentes = asistenciaRepository.findByFecha(fechaAsistencia);
        if (!existentes.isEmpty()) {
            asistenciaRepository.deleteAll(existentes);
        }

        // Crear nuevas asistencias
        for (Alumno alumno : todosLosAlumnos) {
            Asistencia asistencia = new Asistencia();
            asistencia.setIdAlumno(alumno.getIdAlumno());
            asistencia.setFecha(fechaAsistencia);
            
            // Buscar bitácora individual para este alumno
            String bitacoraKey = "bitacora_" + alumno.getIdAlumno();
            String bitacoraIndividual = allParams.get(bitacoraKey);
            
            // Usar bitácora individual si existe, si no usar la general
            if (bitacoraIndividual != null && !bitacoraIndividual.isEmpty()) {
                asistencia.setBitacora(bitacoraIndividual);
            } else {
                asistencia.setBitacora(bitacora != null ? bitacora : "");
            }

            // ✅ CORREGIDO: Si el alumno está en la lista de presentes, marcarlo como Presente
            if (alumnosPresentes != null && alumnosPresentes.contains(alumno.getIdAlumno())) {
                asistencia.setEstado("Presente");
            } else {
                asistencia.setEstado("Ausente"); // ✅ CORREGIDO: Error de sintaxis
            }

            asistenciaRepository.save(asistencia);
        }

        return "redirect:/asistencias?fecha=" + fecha;
    }

    // 📊 OBTENER ESTADÍSTICAS (CORREGIDO)
    @GetMapping("/asistencias/estadisticas")
    @ResponseBody
    public Map<String, Object> getEstadisticas(@RequestParam String fecha) {
        // ✅ CORREGIDO: Quitar "text:" que sobraba
        LocalDate fechaAsistencia = LocalDate.parse(fecha);
        List<Alumno> alumnos = alumnoRepository.findAll();

        // ✅ CORREGIDO: Usar el repositorio inyectado, no la clase estática
        List<Asistencia> asistencias = asistenciaRepository.findByFecha(fechaAsistencia);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", alumnos.size());
        
        // ✅ CORREGIDO: Líneas completas
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
        
        // Calcular estadísticas del alumno
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
}