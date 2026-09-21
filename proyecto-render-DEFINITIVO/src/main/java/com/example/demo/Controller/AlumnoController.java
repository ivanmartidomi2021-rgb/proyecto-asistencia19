package com.example.demo.controller;

import com.example.demo.model.Alumno;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/alumnos")
public class AlumnoController {

    @Autowired
    private AlumnoRepository alumnoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    // Listar alumnos -> muestra templates/alumnos.html
    @GetMapping
    public String listarAlumnos(Model model) {
        model.addAttribute("alumnos", alumnoRepository.findAll());
        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("totalAlumnos", alumnoRepository.count());
        model.addAttribute("alumno", new Alumno());
        return "alumnos";   // ✅ archivo templates/alumnos.html
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