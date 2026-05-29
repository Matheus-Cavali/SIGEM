package org.example.model;

import com.google.gson.Gson;
import org.example.dao.EnderecoDao;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Endereco {

    private Integer id;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;

    private static EnderecoDao dao;

    public static synchronized EnderecoDao getDao() {
        if (dao == null)
            dao = new EnderecoDao();
        return dao;
    }

    public Endereco() {}

    public Endereco(Integer id, String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf) {
        this.id = id;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
    }

    public static Map<String, String> validarEndereco(Endereco e) {
        Map<String, String> erros = new LinkedHashMap<>();

        if (e.getCep() == null || !e.getCep().matches("\\d{5}-\\d{3}"))
            erros.put("cep", "CEP inválido");
        if (e.getLogradouro() == null || e.getLogradouro().trim().isEmpty())
            erros.put("logradouro", "Logradouro é obrigatório");
        if (e.getNumero() == null || e.getNumero().trim().isEmpty())
            erros.put("numero", "Número é obrigatório");
        if (e.getBairro() == null || e.getBairro().trim().isEmpty())
            erros.put("bairro", "Bairro é obrigatório");
        if (e.getCidade() == null || e.getCidade().trim().isEmpty())
            erros.put("cidade", "Cidade é obrigatória");
        if (e.getUf() == null || !e.getUf().matches("[A-Z]{2}"))
            erros.put("uf", "UF deve ter 2 caracteres");

        return erros;
    }

    public Integer cadastrar(Connection conn, Endereco e) {
        Map<String, String> erros = validarEndereco(e);
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        return getDao().cadastrar(conn, e);
    }

    public void atualizar(Connection conn, Endereco e) {
        if (e.getId() == null || e.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");
        Map<String, String> erros = validarEndereco(e);
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getDao().atualizar(conn, e);
    }

    public void salvar(Connection conn, Endereco e) {
        if (e.getId() != null)
            atualizar(conn, e);
        else
            e.setId(cadastrar(conn, e));
    }

    public void excluir(Connection conn, Integer id) {
        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        getDao().excluir(conn, id);
    }

    public Endereco buscarPorId(Connection conn, Integer id) {
        if (id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        return getDao().buscarPorId(conn, id);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
}
