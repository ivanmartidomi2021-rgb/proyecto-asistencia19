package com.example.demo.Controller;

import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.EspecialidadRepository;
import com.example.demo.repository.MateriasRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private AlumnoRepository alumnoRepository;
    @Autowired
    private MateriasRepository materiaRepository;
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalAlumnos",      alumnoRepository.count());
        model.addAttribute("totalMaterias",     materiaRepository.count());
        model.addAttribute("totalAsistencias",  asistenciaRepository.count());
        model.addAttribute("totalEspecialidades", especialidadRepository.count());
        model.addAttribute("totalUsuarios",     usuarioRepository.count());

        // ← esta es la línea que faltaba
        model.addAttribute("listaAlumnos",      alumnoRepository.findAll());

        return "dashboard";
    }
}