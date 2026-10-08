package com.example.demo.repository;

import com.example.demo.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    List<Asistencia> findByFecha(LocalDate fecha);

    List<Asistencia> findByIdAlumno(Integer idAlumno);

    List<Asistencia> findByIdAlumnoAndFechaBetween(
            Integer idAlumno, LocalDate inicio, LocalDate fin);

    List<Asistencia> findByFechaBetween(LocalDate inicio, LocalDate fin);
}
