package com.porto.testecnae.repository;

import com.porto.testecnae.domain.CadastroSecundario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CadastroSecundarioRepository extends JpaRepository<CadastroSecundario, Long> {

    @Override
    @EntityGraph(attributePaths = "cnae")
    List<CadastroSecundario> findAll();
}
