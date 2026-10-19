package com.porto.testecnae.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CnaeApiIntegrationTest {

    private static final String CADASTRO_VALIDO =
            "{\"nomeFantasia\":\"Tech Porto\",\"documento\":\"12345678000199\",\"codigoCnae\":\"%s\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    void listaTodosOsCnaes() throws Exception {
        mvc.perform(get("/api/cnaes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
    }

    @Test
    void buscaPorTermoEmQualquerParteDaDescricaoIgnorandoCase() throws Exception {
        mvc.perform(get("/api/cnaes/buscar").param("termo", "PROGRAMAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].codigo").value("6201-5/01"));
    }

    @Test
    void buscaTrataCuringaLikeComoTextoLiteral() throws Exception {
        mvc.perform(get("/api/cnaes/buscar").param("termo", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void buscaSemTermoRetorna400() throws Exception {
        mvc.perform(get("/api/cnaes/buscar").param("termo", " "))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/cnaes/buscar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscaPorCodigoExistente() throws Exception {
        mvc.perform(get("/api/cnaes/codigo").param("codigo", "6201-5/01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secao").value("Tecnologia"));
    }

    @Test
    void buscaPorCodigoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/cnaes/codigo").param("codigo", "9999-9/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void validarCnaeExistenteRetorna200EInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/cadastros-secundarios/validar-cnae").param("codigoCnae", "6201-5/01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("6201-5/01"));
        mvc.perform(get("/api/cadastros-secundarios/validar-cnae").param("codigoCnae", "9999-9/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void criaCadastroVinculadoAoCnaeELista() throws Exception {
        mvc.perform(post("/api/cadastros-secundarios").contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO_VALIDO.formatted("6201-5/01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cnae.codigo").value("6201-5/01"));

        mvc.perform(get("/api/cadastros-secundarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cnae.descricao").exists());
    }

    @Test
    void cadastroComCnaeInexistenteEBarradoENaoPersiste() throws Exception {
        mvc.perform(post("/api/cadastros-secundarios").contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRO_VALIDO.formatted("9999-9/99")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/cadastros-secundarios"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cadastroComPayloadInvalidoRetorna400ComDetalheDosCampos() throws Exception {
        mvc.perform(post("/api/cadastros-secundarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documento\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nomeFantasia").exists())
                .andExpect(jsonPath("$.erros.documento").exists())
                .andExpect(jsonPath("$.erros.codigoCnae").exists());
    }

    @Test
    void cadastroComJsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/cadastros-secundarios").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
