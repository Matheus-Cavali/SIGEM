package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Despesa {
    private int id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataLancamento;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private int categoriaDespesaId;
    private String categoriaDespesaNome;
    private Integer colaboradorLancouId;

    public Despesa() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDataLancamento() { return dataLancamento; }
    public void setDataLancamento(LocalDate dataLancamento) { this.dataLancamento = dataLancamento; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }

    public int getCategoriaDespesaId() { return categoriaDespesaId; }
    public void setCategoriaDespesaId(int categoriaDespesaId) { this.categoriaDespesaId = categoriaDespesaId; }

    public String getCategoriaDespesaNome() { return categoriaDespesaNome; }
    public void setCategoriaDespesaNome(String categoriaDespesaNome) { this.categoriaDespesaNome = categoriaDespesaNome; }

    public Integer getColaboradorLancouId() { return colaboradorLancouId; }
    public void setColaboradorLancouId(Integer colaboradorLancouId) { this.colaboradorLancouId = colaboradorLancouId; }
}
