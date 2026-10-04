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

            Usuario admin = usuarioRepository.findByUsername("admin");

            if (admin == null) {
                // No existe → lo creamos
                admin = new Usuario();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode("123456"));
                admin.setNombre("Administrador");
                admin.setActivo(true);
                admin.setCreadoEn(LocalDateTime.now());
                usuarioRepository.save(admin);

                System.out.println(">>> ============================================");
                System.out.println(">>> Usuario admin CREADO: admin / 123456");
                System.out.println(">>> ============================================");
            } else if (admin.getPasswordHash() == null 
                    || !admin.getPasswordHash().startsWith("$2")) {
                // Existe pero su contraseña NO es un hash BCrypt → la corregimos
                admin.setPasswordHash(encoder.encode("123456"));
                admin.setActivo(true);
                usuarioRepository.save(admin);

                System.out.println(">>> ============================================");
                System.out.println(">>> Contraseña del admin ACTUALIZADA a hash BCrypt");
                System.out.println(">>> Login: admin / 123456");
                System.out.println(">>> ============================================");
            } else {
                System.out.println(">>> Usuario admin ya existe con hash válido.");
            }
        };
    }
}
