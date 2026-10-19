package com.porto.testecnae.service;

import com.porto.testecnae.domain.AtividadeEconomicaCnae;
import com.porto.testecnae.domain.CadastroSecundario;
import com.porto.testecnae.dto.CadastroSecundarioRequest;
import com.porto.testecnae.exception.CnaeNaoEncontradoException;
import com.porto.testecnae.repository.AtividadeEconomicaCnaeRepository;
import com.porto.testecnae.repository.CadastroSecundarioRepository;
import com.porto.testecnae.service.impl.CadastroSecundarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroSecundarioServiceImplTest {

    @Mock
    private CadastroSecundarioRepository repository;

    @Mock
    private AtividadeEconomicaCnaeRepository cnaeRepository;

    @InjectMocks
    private CadastroSecundarioServiceImpl service;

    @Test
    void cadastrarVinculaCnaeExistente() {
        var cnae = AtividadeEconomicaCnae.builder().id(5L).codigo("6201-5/01").descricao("Dev").secao("TI").build();
        when(cnaeRepository.findByCodigo("6201-5/01")).thenReturn(Optional.of(cnae));
        when(repository.save(any(CadastroSecundario.class))).thenAnswer(inv -> {
            CadastroSecundario c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        var response = service.cadastrar(new CadastroSecundarioRequest("Tech Porto", "12345678000199", "6201-5/01"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.cnae().codigo()).isEqualTo("6201-5/01");
    }

    @Test
    void cadastrarComCnaeInexistenteNaoPersiste() {
        when(cnaeRepository.findByCodigo("9999-9/99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(new CadastroSecundarioRequest("X", "12345678000199", "9999-9/99")))
                .isInstanceOf(CnaeNaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void validarCnaeInexistenteLancaExcecao() {
        when(cnaeRepository.findByCodigo("9999-9/99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarCnae("9999-9/99"))
                .isInstanceOf(CnaeNaoEncontradoException.class);
    }
}
