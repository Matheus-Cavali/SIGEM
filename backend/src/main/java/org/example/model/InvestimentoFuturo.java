package org.example.model;

import com.google.gson.Gson;
import org.example.dao.InvestimentoFuturoDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InvestimentoFuturo {
    private Integer id;
    private String nome;
    private BigDecimal valorMeta;
    private LocalDate dataAbertura;
    private String status;
    private int colaboradorId;
    private String colaboradorNome;
    private BigDecimal saldoAtual;

    private static InvestimentoFuturoDao dao;

    public static synchronized InvestimentoFuturoDao getInvestimentoFuturoDao() {
        if (dao == null) dao = new InvestimentoFuturoDao();
        return dao;
    }

    public InvestimentoFuturo() {}

    public Map<String, String> validar() {
        Map<String, String> erros = new LinkedHashMap<>();
        if (nome == null || nome.trim().isEmpty())
            erros.put("nome", "Nome do investimento é obrigatório");
        if (valorMeta == null || valorMeta.compareTo(BigDecimal.ZERO) <= 0)
            erros.put("valorMeta", "Valor meta deve ser maior que zero");
        if (dataAbertura == null)
            erros.put("dataAbertura", "Data de abertura é obrigatória");
        return erros;
    }

    public void cadastrar(Connection conn, InvestimentoFuturo inv) {
        Map<String, String> erros = inv.validar();
        InvestimentoFuturo existente = getInvestimentoFuturoDao().buscarPorNome(conn, inv.getNome().trim());
        if (existente != null) erros.put("nome", "Já existe um investimento com este nome");
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        int id = getInvestimentoFuturoDao().inserir(conn, inv);
        inv.setId(id);
    }

    public void alterar(Connection conn, InvestimentoFuturo inv) {
        Map<String, String> erros = inv.validar();
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        getInvestimentoFuturoDao().atualizar(conn, inv);
    }

    public void excluir(Connection conn, Integer id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("ID inválido.");
        getInvestimentoFuturoDao().deletar(conn, id);
    }

    public InvestimentoFuturo buscarPorId(Connection conn, Integer id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("ID inválido.");
        return getInvestimentoFuturoDao().buscarPorId(conn, id);
    }

    public InvestimentoFuturo buscarPorNome(Connection conn, String nome) {
        return getInvestimentoFuturoDao().buscarPorNome(conn, nome);
    }

    public BigDecimal calcularSaldo(Connection conn, int investimentoId) {
        return getInvestimentoFuturoDao().calcularSaldo(conn, investimentoId);
    }

    public List<InvestimentoFuturo> filtrar(Connection conn, String nome, String status) {
        return getInvestimentoFuturoDao().listar(conn, nome, status);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getValorMeta() { return valorMeta; }
    public void setValorMeta(BigDecimal valorMeta) { this.valorMeta = valorMeta; }
    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getColaboradorId() { return colaboradorId; }
    public void setColaboradorId(int colaboradorId) { this.colaboradorId = colaboradorId; }
    public String getColaboradorNome() { return colaboradorNome; }
    public void setColaboradorNome(String colaboradorNome) { this.colaboradorNome = colaboradorNome; }
    public BigDecimal getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }
}
