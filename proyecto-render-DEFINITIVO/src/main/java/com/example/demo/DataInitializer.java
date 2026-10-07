package com.example.demo;

import com.example.demo.model.Especialidad;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EspecialidadRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    // ============================================================
    // 1) Inicializar usuario admin (YA LO TENÍAS)
    // ============================================================
    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            Usuario admin = usuarioRepository.findByUsername("admin");

            if (admin == null) {
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

    // ============================================================
    // 2) Inicializar materias de un instituto salvadoreño (NUEVO)
    // ============================================================
    @Bean
    public CommandLineRunner initEspecialidades(EspecialidadRepository especialidadRepository) {
        return args -> {
            if (especialidadRepository.count() == 0) {
                especialidadRepository.saveAll(List.of(
                    new Especialidad("Matemática"),
                    new Especialidad("Lenguaje y Literatura"),
                    new Especialidad("Ciencias Naturales"),
                    new Especialidad("Estudios Sociales y Cívica"),
                    new Especialidad("Inglés"),
                    new Especialidad("Educación Física"),
                    new Especialidad("Educación Artística"),
                    new Especialidad("Informática")
                ));

                System.out.println(">>> ============================================");
                System.out.println(">>> 8 materias del instituto CARGADAS.");
                System.out.println(">>> ============================================");
            } else {
                System.out.println(">>> Las materias ya están registradas ("
                        + especialidadRepository.count() + " en total).");
            }
        };
    }
}
