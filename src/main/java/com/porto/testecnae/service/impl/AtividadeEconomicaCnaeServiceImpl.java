package com.porto.testecnae.service.impl;

import com.porto.testecnae.dto.AtividadeEconomicaCnaeResponse;
import com.porto.testecnae.exception.CnaeNaoEncontradoException;
import com.porto.testecnae.repository.AtividadeEconomicaCnaeRepository;
import com.porto.testecnae.service.AtividadeEconomicaCnaeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtividadeEconomicaCnaeServiceImpl implements AtividadeEconomicaCnaeService {

    private final AtividadeEconomicaCnaeRepository repository;

    @Override
    public List<AtividadeEconomicaCnaeResponse> listarTodas() {
        return repository.findAll()
                .stream()
                .map(AtividadeEconomicaCnaeResponse::fromEntity)
                .toList();
    }

    @Override
    public List<AtividadeEconomicaCnaeResponse> buscarPorDescricao(String termo) {
        return repository.findByDescricaoContainingIgnoreCaseOrderByCodigo(termo.trim())
                .stream()
                .map(AtividadeEconomicaCnaeResponse::fromEntity)
                .toList();
    }

    @Override
    public AtividadeEconomicaCnaeResponse buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo.trim())
                .map(AtividadeEconomicaCnaeResponse::fromEntity)
                .orElseThrow(() -> new CnaeNaoEncontradoException(codigo));
    }
}
