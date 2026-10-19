package com.porto.testecnae.service;

import com.porto.testecnae.domain.AtividadeEconomicaCnae;
import com.porto.testecnae.exception.CnaeNaoEncontradoException;
import com.porto.testecnae.repository.AtividadeEconomicaCnaeRepository;
import com.porto.testecnae.service.impl.AtividadeEconomicaCnaeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtividadeEconomicaCnaeServiceImplTest {

    @Mock
    private AtividadeEconomicaCnaeRepository repository;

    @InjectMocks
    private AtividadeEconomicaCnaeServiceImpl service;

    private static AtividadeEconomicaCnae cnae() {
        return AtividadeEconomicaCnae.builder()
                .id(5L).codigo("6201-5/01").descricao("Desenvolvimento de programas").secao("Tecnologia").build();
    }

    @Test
    void buscarPorCodigoRetornaCnaeExistente() {
        when(repository.findByCodigo("6201-5/01")).thenReturn(Optional.of(cnae()));

        assertThat(service.buscarPorCodigo("6201-5/01").codigo()).isEqualTo("6201-5/01");
    }

    @Test
    void buscarPorCodigoInexistenteLancaExcecaoSemFallback() {
        when(repository.findByCodigo("9999-9/99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorCodigo("9999-9/99"))
                .isInstanceOf(CnaeNaoEncontradoException.class);
        verify(repository).findByCodigo("9999-9/99");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void buscarPorDescricaoDelegaTermoSemEspacos() {
        when(repository.findByDescricaoContainingIgnoreCaseOrderByCodigo("programas")).thenReturn(List.of(cnae()));

        assertThat(service.buscarPorDescricao("  programas ")).hasSize(1);
    }
}
