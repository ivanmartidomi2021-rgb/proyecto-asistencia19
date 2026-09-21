package com.example.demo.Controller;

import com.example.demo.repository.EspecialidadRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EspecialidadController {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @GetMapping("/especialidades")
    public String especialidades(Model model){

        model.addAttribute(
                "listaEspecialidades",
                especialidadRepository.findAll()
        );

        return "especialidades";
    }
}