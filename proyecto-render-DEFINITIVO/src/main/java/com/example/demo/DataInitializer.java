package com.example.demo;

import com.example.demo.model.Especialidad;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EspecialidadRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    // ============================================================
    // Hash BCrypt pre-generado para la contraseña: 123456
    // (No se puede revertir a texto plano a simple vista)
    // ============================================================
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    // ============================================================
    // 1) Inicializar usuario admin
    // ============================================================
    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository) {
        return args -> {
            Usuario admin = usuarioRepository.findByUsername("admin");

            if (admin == null) {
                admin = new Usuario();
                admin.setUsername("admin");
                admin.setPasswordHash(ADMIN_PASSWORD_HASH);
                admin.setNombre("Administrador");
                admin.setActivo(true);
                admin.setCreadoEn(LocalDateTime.now());
                usuarioRepository.save(admin);

                System.out.println(">>> ============================================");
                System.out.println(">>> Usuario admin creado correctamente.");
                System.out.println(">>> ============================================");
            } else if (admin.getPasswordHash() == null
                    || !admin.getPasswordHash().startsWith("$2")) {
                admin.setPasswordHash(ADMIN_PASSWORD_HASH);
                admin.setActivo(true);
                usuarioRepository.save(admin);

                System.out.println(">>> ============================================");
                System.out.println(">>> Contraseña del admin actualizada a hash BCrypt.");
                System.out.println(">>> ============================================");
            } else {
                System.out.println(">>> Usuario admin ya existe con hash válido.");
            }
        };
    }

    // ============================================================
    // 2) Inicializar materias de un instituto salvadoreño
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
                System.out.println(">>> 8 materias del instituto cargadas.");
                System.out.println(">>> ============================================");
            } else {
                System.out.println(">>> Las materias ya están registradas ("
                        + especialidadRepository.count() + " en total).");
            }
        };
    }
}
