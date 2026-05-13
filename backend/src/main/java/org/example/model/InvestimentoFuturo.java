package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvestimentoFuturo {
    private int id;
    private String nome;
    private BigDecimal valorMeta;
    private LocalDate dataAbertura;
    private String status;
    private int colaboradorId;
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

    public BigDecimal getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }
}