package com.porto.testecnae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroSecundarioRequest(
        @NotBlank(message = "nomeFantasia e obrigatorio")
        @Size(max = 120, message = "nomeFantasia deve ter no maximo 120 caracteres")
        String nomeFantasia,

        @NotBlank(message = "documento e obrigatorio")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "documento deve conter 11 (CPF) ou 14 (CNPJ) digitos numericos")
        String documento,

        @NotBlank(message = "codigoCnae e obrigatorio")
        String codigoCnae
) {
}
