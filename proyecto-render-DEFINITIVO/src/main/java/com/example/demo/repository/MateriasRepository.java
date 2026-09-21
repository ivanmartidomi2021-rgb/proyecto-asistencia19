package com.example.demo.repository;

import com.example.demo.model.Materias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MateriasRepository
        extends JpaRepository<Materias, Integer> {

}