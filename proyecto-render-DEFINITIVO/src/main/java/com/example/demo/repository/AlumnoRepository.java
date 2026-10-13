package com.example.demo.repository;

import com.example.demo.model.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlumnoRepository extends JpaRepository<Alumno, Integer> {

    // 🔍 Búsqueda por nombre o apellido (ignora mayúsculas/minúsculas)
    List<Alumno> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String nombres, String apellidos);

    // 📊 Orden alfabético
    List<Alumno> findAllByOrderByNombresAsc();
    List<Alumno> findAllByOrderByApellidosAsc();
    List<Alumno> findAllByOrderByGradoAsc();
}
