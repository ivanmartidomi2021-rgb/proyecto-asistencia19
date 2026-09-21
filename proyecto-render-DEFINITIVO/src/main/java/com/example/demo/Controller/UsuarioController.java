package com.example.demo.Controller;

import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/usuarios")
    public String usuarios(Model model){

        model.addAttribute(
                "listaUsuarios",
                usuarioRepository.findAll()
        );

        return "usuarios";
    }
}