package com.example.demo.Controller;

import com.example.demo.repository.MateriasRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MateriasController {

    @Autowired
    private MateriasRepository materiasRepository;

    @GetMapping("/materias")
    public String materias(Model model){

        model.addAttribute(
                "listaMaterias",
                materiasRepository.findAll()
        );

        return "materias";
    }
}