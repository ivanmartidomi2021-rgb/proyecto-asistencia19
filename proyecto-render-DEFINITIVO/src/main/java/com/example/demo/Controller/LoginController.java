package com.example.demo.Controller;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ✅ Mostrar formulario
    @GetMapping("/login")
    public String loginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (logout != null) {
            model.addAttribute("error", "Has cerrado sesión");
        }

        return "login";
    }

    // ✅ Procesar login (validación contra la BD)
    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // 🔍 Buscar usuario en la BD
        Usuario usuario = usuarioRepository.findByUsername(username);

        // ❌ Usuario no existe
        if (usuario == null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }

        // ❌ Usuario inactivo
        if (usuario.getActivo() == null || !usuario.getActivo()) {
            model.addAttribute("error", "Usuario inactivo");
            return "login";
        }

        // ✅ Validar contraseña con BCrypt
        if (!encoder.matches(password, usuario.getPasswordHash())) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }

        // ✅ Login OK → guardar en sesión
        session.setAttribute("usuarioId", usuario.getId_usuario());
        session.setAttribute("usuarioNombre", usuario.getNombre());
        session.setAttribute("usuarioUsername", usuario.getUsername());

        return "redirect:/dashboard";
    }

    // ✅ Cerrar sesión
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
