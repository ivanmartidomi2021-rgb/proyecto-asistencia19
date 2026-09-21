package com.example.demo.Controller;

import com.example.demo.model.Bitacora;
import com.example.demo.repository.BitacoraRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BitacoraController {

    @Autowired
    private BitacoraRepository bitacoraRepository;

    // MOSTRAR BITACORA
    @GetMapping("/bitacora")
    public String mostrarBitacora(Model model){

        model.addAttribute(
                "listaBitacora",
                bitacoraRepository.findAll()
        );

        return "bitacora";
    }

}