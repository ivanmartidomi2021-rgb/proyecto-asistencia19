package com.example.demo;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository) {
        return args -> {
            // Verificamos si la tabla está vacía
            if (usuarioRepository.count() == 0) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                // Asegúrate de cambiar esto por la contraseña que necesites
                admin.setPasswordHash("123456"); 
                admin.setNombre("Administrador");
                admin.setActivo(true);
                admin.setCreadoEn(LocalDateTime.now());

                usuarioRepository.save(admin);
                System.out.println(">>> Usuario administrador creado exitosamente en Render <<<");
            }
        };
    }
}
