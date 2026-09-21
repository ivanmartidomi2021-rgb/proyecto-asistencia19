package com.example.demo.repository;

import com.example.demo.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    // 📋 Buscar asistencias por fecha
    List<Asistencia> findByFecha(LocalDate fecha);
    
    // 📋 Buscar asistencias por fecha y estado
    List<Asistencia> findByFechaAndEstado(LocalDate fecha, String estado);
    
    // 📋 Buscar asistencias por alumno
    List<Asistencia> findByIdAlumno(Integer idAlumno);
    
    // 📋 Buscar asistencias por alumno y fecha
    List<Asistencia> findByIdAlumnoAndFecha(Integer idAlumno, LocalDate fecha);
    
    // 📊 Contar asistencias por fecha y estado
    long countByFechaAndEstado(LocalDate fecha, String estado);
    
    // 📊 Contar asistencias de un alumno por estado
    long countByIdAlumnoAndEstado(Integer idAlumno, String estado);
    
    // 🗑️ Eliminar asistencias por fecha (USAR CON CUIDADO)
    @Modifying
    @Transactional
    @Query("DELETE FROM Asistencia a WHERE a.fecha = :fecha")
    void deleteByFecha(@Param("fecha") LocalDate fecha);
    
    // 📊 Estadísticas mensuales
    @Query("SELECT YEAR(a.fecha) as anio, MONTH(a.fecha) as mes, " +
           "COUNT(CASE WHEN a.estado = 'Presente' THEN 1 END) as presentes, " +
           "COUNT(CASE WHEN a.estado = 'Ausente' THEN 1 END) as ausentes " +
           "FROM Asistencia a " +
           "WHERE a.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY YEAR(a.fecha), MONTH(a.fecha) " +
           "ORDER BY anio DESC, mes DESC")
    List<Object[]> getEstadisticasMensuales(@Param("fechaInicio") LocalDate fechaInicio,
                                            @Param("fechaFin") LocalDate fechaFin);
    
    // 📊 Top 5 alumnos con más ausencias
    @Query("SELECT a.idAlumno, COUNT(a) as ausencias " +
           "FROM Asistencia a " +
           "WHERE a.estado = 'Ausente' " +
           "AND a.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY a.idAlumno " +
           "ORDER BY ausencias DESC")
    List<Object[]> findTopAusentes(@Param("fechaInicio") LocalDate fechaInicio,
                                   @Param("fechaFin") LocalDate fechaFin);
}