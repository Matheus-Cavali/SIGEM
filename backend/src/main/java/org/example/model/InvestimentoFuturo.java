package org.example.model;

import org.example.dao.InvestimentoFuturoDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class InvestimentoFuturo {
    private int id;
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

    public static int salvar(Connection conn, InvestimentoFuturo inv) throws SQLException {
        return getInvestimentoFuturoDao().inserir(conn, inv);
    }

    public static InvestimentoFuturo buscarPorId(Connection conn, int id) throws SQLException {
        return getInvestimentoFuturoDao().buscarPorId(conn, id);
    }

    public static InvestimentoFuturo buscarPorNome(Connection conn, String nome) throws SQLException {
        return getInvestimentoFuturoDao().buscarPorNome(conn, nome);
    }

    public static boolean atualizar(Connection conn, InvestimentoFuturo inv) throws SQLException {
        return getInvestimentoFuturoDao().atualizar(conn, inv);
    }

    public static boolean deletar(Connection conn, int id) throws SQLException {
        return getInvestimentoFuturoDao().deletar(conn, id);
    }

    public static BigDecimal calcularSaldo(Connection conn, int investimentoId) throws SQLException {
        return getInvestimentoFuturoDao().calcularSaldo(conn, investimentoId);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
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