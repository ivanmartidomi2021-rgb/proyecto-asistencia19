package com.example.demo.Controller;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class LoginController {
    @Autowired
    private UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @GetMapping("/")
    public String inicio() {
        return "redirect:/login";
    }
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }
    @PostMapping("/login")
    public String validarLogin(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {
        Usuario user = usuarioRepository.findByUsername(username);

        // DEBUG
        System.out.println("USER: " + (user != null ? user.getUsername() : "null"));
        System.out.println("HASH: " + (user != null ? user.getPasswordHash() : "null"));
        System.out.println("ACTIVO: " + (user != null ? user.getActivo() : "null"));
        System.out.println("MATCH: " + (user != null ? encoder.matches(password, user.getPasswordHash()) : "null"));

        if (user != null
                && user.getPasswordHash() != null
                && encoder.matches(password, user.getPasswordHash())
                && Boolean.TRUE.equals(user.getActivo())) {
            model.addAttribute("nombre", user.getNombre());
            return "dashboard";
        }
        model.addAttribute("error", "Credenciales incorrectas");
        return "login";
    }
}