package com.porto.testecnae.service.impl;

import com.porto.testecnae.domain.AtividadeEconomicaCnae;
import com.porto.testecnae.domain.CadastroSecundario;
import com.porto.testecnae.dto.AtividadeEconomicaCnaeResponse;
import com.porto.testecnae.dto.CadastroSecundarioRequest;
import com.porto.testecnae.dto.CadastroSecundarioResponse;
import com.porto.testecnae.exception.CnaeNaoEncontradoException;
import com.porto.testecnae.repository.AtividadeEconomicaCnaeRepository;
import com.porto.testecnae.repository.CadastroSecundarioRepository;
import com.porto.testecnae.service.CadastroSecundarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CadastroSecundarioServiceImpl implements CadastroSecundarioService {

    private final CadastroSecundarioRepository repository;
    private final AtividadeEconomicaCnaeRepository cnaeRepository;

    @Override
    @Transactional
    public CadastroSecundarioResponse cadastrar(CadastroSecundarioRequest request) {
        var cnae = buscarCnaeExistente(request.codigoCnae());

        var cadastro = CadastroSecundario.builder()
                .nomeFantasia(request.nomeFantasia().trim())
                .documento(request.documento())
                .cnae(cnae)
                .build();

        return CadastroSecundarioResponse.fromEntity(repository.save(cadastro));
    }

    @Override
    public AtividadeEconomicaCnaeResponse validarCnae(String codigoCnae) {
        return AtividadeEconomicaCnaeResponse.fromEntity(buscarCnaeExistente(codigoCnae));
    }

    @Override
    public List<CadastroSecundarioResponse> listarTodos() {
        return repository.findAll()
                .stream()
                .map(CadastroSecundarioResponse::fromEntity)
                .toList();
    }

    private AtividadeEconomicaCnae buscarCnaeExistente(String codigoCnae) {
        return cnaeRepository.findByCodigo(codigoCnae.trim())
                .orElseThrow(() -> new CnaeNaoEncontradoException(codigoCnae));
    }
}
