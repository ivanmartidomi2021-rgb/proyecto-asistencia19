package com.example.demo;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            
            // Buscamos si el usuario 'admin' ya existe en la base de datos de Render
            Usuario admin = usuarioRepository.findByUsername("admin");

            if (admin == null) {
                // Si no existe, lo creamos
                admin = new Usuario();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("123456")); // Contraseña encriptada
                admin.setNombre("Administrador");
                admin.setActivo(true);
                admin.setCreadoEn(LocalDateTime.now());
                
                usuarioRepository.save(admin);
                
                System.out.println(">>> ============================================");
                System.out.println(">>> ¡USUARIO ADMIN CREADO EN RENDER!");
                System.out.println(">>> Usuario: admin");
                System.out.println(">>> Contraseña: 123456");
                System.out.println(">>> ============================================");
            } else {
                System.out.println(">>> El usuario admin ya existe en Render.");
            }
        };
    }
}
