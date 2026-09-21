package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "materias")
public class Materias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id_materia")
    private Integer idMateria;

    @Column(name = "nombre")
    private String nombre;

    public Materias() {
    }

    public Integer getIdMateria() {
        return idMateria;
    }

    public void setIdMateria(Integer idMateria) {
        this.idMateria = idMateria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}