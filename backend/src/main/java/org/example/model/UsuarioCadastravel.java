package org.example.model;

import com.google.gson.Gson;
import org.example.util.Criptografia;

import java.sql.Connection;
import java.util.Map;

public abstract class UsuarioCadastravel extends Usuario {

    public final void cadastrarCompleto(Connection conn) {
        validarDados();
        limparCpf();
        prepararSenha();
        definirNivelAcessoPadrao();
        this.cadastrar(conn, this);
        this.cadastrarPapel(conn);
    }

    private void validarDados() {
        Map<String, String> erros = this.validar();
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
    }

    private void limparCpf() {
        String cpf = getCpf();
        setCpf(cpf != null ? cpf.replaceAll("[^0-9]", "") : "");
    }

    private void prepararSenha() {
        setSenha(Criptografia.hashSenha(getSenha()));
    }

    protected void definirNivelAcessoPadrao() {
        if (getNivelAcesso() <= 0) setNivelAcesso(2);
    }

    protected abstract void cadastrarPapel(Connection conn);
}