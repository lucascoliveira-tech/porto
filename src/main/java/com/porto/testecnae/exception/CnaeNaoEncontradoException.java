package com.porto.testecnae.exception;

public class CnaeNaoEncontradoException extends RuntimeException {

    public CnaeNaoEncontradoException(String codigo) {
        super("CNAE nao encontrado para o codigo: " + codigo);
    }
}
