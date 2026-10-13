package com.example.demo.controller;

import com.example.demo.model.Alumno;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/alumnos")
public class AlumnoController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    // Listar alumnos con búsqueda y orden
    @GetMapping
    public String listarAlumnos(
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "orden", required = false, defaultValue = "id") String orden,
            Model model) {

        List<Alumno> alumnos;

        // 🔍 Búsqueda por nombre o apellido
        if (buscar != null && !buscar.trim().isEmpty()) {
            alumnos = alumnoRepository
                    .findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(buscar, buscar);
        } else {
            // 📊 Orden alfabético
            switch (orden) {
                case "nombre":
                    alumnos = alumnoRepository.findAllByOrderByNombresAsc();
                    break;
                case "apellido":
                    alumnos = alumnoRepository.findAllByOrderByApellidosAsc();
                    break;
                case "grado":
                    alumnos = alumnoRepository.findAllByOrderByGradoAsc();
                    break;
                default:
                    alumnos = alumnoRepository.findAll();
            }
        }

        model.addAttribute("alumnos", alumnos);
        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("totalAlumnos", alumnoRepository.count());
        model.addAttribute("alumno", new Alumno());
        model.addAttribute("buscar", buscar);   // para mantener el valor en el input
        model.addAttribute("orden", orden);     // para mantener el select seleccionado
        return "alumnos";
    }

    // Guardar alumno (nuevo o edición)
    @PostMapping("/guardar")
    public String guardarAlumno(@ModelAttribute Alumno alumno) {
        alumnoRepository.save(alumno);
        return "redirect:/alumnos";
    }

    // Eliminar alumno
    @GetMapping("/eliminar/{id}")
    public String eliminarAlumno(@PathVariable Integer id) {
        alumnoRepository.deleteById(id);
        return "redirect:/alumnos";
    }
}
