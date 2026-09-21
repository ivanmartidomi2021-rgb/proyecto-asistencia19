package com.example.demo.repository;

import com.example.demo.model.Bitacora;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepository
        extends JpaRepository<Bitacora, Integer> {

}