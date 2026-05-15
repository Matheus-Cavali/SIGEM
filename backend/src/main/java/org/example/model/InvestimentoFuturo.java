package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.dao.InvestimentoFuturoDao;
import org.example.util.Data;

public class InvestimentoFuturo {
    private int id;
    private String nome;
    private BigDecimal valorMeta;
    private LocalDate dataAbertura;
    private String status;
    private int colaboradorId;
    private String colaboradorNome;
    private BigDecimal saldoAtual;

    public InvestimentoFuturo() {}

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

    public String validar(String dataStr) {
        if (nome == null || nome.trim().isEmpty()) {
            return "Nome do investimento \u00e9 obrigat\u00f3rio";
        }
        if (valorMeta == null || valorMeta.compareTo(BigDecimal.ZERO) <= 0) {
            return "Valor meta deve ser maior que zero";
        }
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return "Data de abertura \u00e9 obrigat\u00f3ria";
        }
        if (Data.parseFlexivel(dataStr) == null) {
            return "Data inv\u00e1lida. Use o formato dd/mm/aaaa ou ddmmaaaa";
        }
        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        InvestimentoFuturo existente = dao.buscarPorNome(nome.trim());
        if (existente != null) {
            return "J\u00e1 existe um investimento com este nome";
        }
        return null;
    }

    public int salvar() {
        return new InvestimentoFuturoDao().inserir(this);
    }
}