package com.example.demo.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    // ✅ Solo mostramos la vista del login
    @GetMapping("/login")
    public String loginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (logout != null) {
            model.addAttribute("error", "Has cerrado sesión correctamente");
        }

        return "login";
    }

    // ✅ Si alguien entra a la raíz, lo mandamos al dashboard (que pedirá login si no está autenticado)
    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
}
