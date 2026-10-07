package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidades")
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @Column(nullable = false, length = 100)
    private String nombre;

    // ✅ Constructor vacío (obligatorio para JPA)
    public Especialidad() {}

    // ✅ ESTE ES EL QUE FALTA – constructor con nombre
    public Especialidad(String nombre) {
        this.nombre = nombre;
    }

    public Integer getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(Integer idEspecialidad) { this.idEspecialidad = idEspecialidad; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
