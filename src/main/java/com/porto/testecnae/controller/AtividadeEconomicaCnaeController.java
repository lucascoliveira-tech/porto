package com.porto.testecnae.controller;

import com.porto.testecnae.dto.AtividadeEconomicaCnaeResponse;
import com.porto.testecnae.service.AtividadeEconomicaCnaeService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cnaes")
public class AtividadeEconomicaCnaeController {

    private final AtividadeEconomicaCnaeService service;

    @GetMapping
    public List<AtividadeEconomicaCnaeResponse> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/buscar")
    public List<AtividadeEconomicaCnaeResponse> buscarPorDescricao(@RequestParam @NotBlank String termo) {
        return service.buscarPorDescricao(termo);
    }

    @GetMapping("/codigo")
    public AtividadeEconomicaCnaeResponse buscarPorCodigo(@RequestParam @NotBlank String codigo) {
        return service.buscarPorCodigo(codigo);
    }
}
