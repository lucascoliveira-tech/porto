package com.porto.testecnae.repository;

import com.porto.testecnae.domain.AtividadeEconomicaCnae;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtividadeEconomicaCnaeRepository extends JpaRepository<AtividadeEconomicaCnae, Long> {

    Optional<AtividadeEconomicaCnae> findByCodigo(String codigo);

    // Containing escapa os curingas (% e _) do termo, evitando LIKE injection
    List<AtividadeEconomicaCnae> findByDescricaoContainingIgnoreCaseOrderByCodigo(String termo);
}
