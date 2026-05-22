package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DoacaoFinanceira extends Doacao {
    private BigDecimal valor;
    private Integer categoriaFinanceiraId;
    private Integer caixaId;

    public DoacaoFinanceira() {
        super();
    }

    public DoacaoFinanceira(Integer id, LocalDate data, Integer colaboradorId, BigDecimal valor, Integer categoriaFinanceiraId, Integer caixaId) {
        super(id, data, colaboradorId);
        this.valor = valor;
        this.categoriaFinanceiraId = categoriaFinanceiraId;
        this.caixaId = caixaId;
    }

    public DoacaoFinanceira(LocalDate data, Integer colaboradorId, BigDecimal valor, Integer categoriaFinanceiraId, Integer caixaId) {
        super(data, colaboradorId);
        this.valor = valor;
        this.categoriaFinanceiraId = categoriaFinanceiraId;
        this.caixaId = caixaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getCategoriaFinanceiraId() {
        return categoriaFinanceiraId;
    }

    public void setCategoriaFinanceiraId(Integer categoriaFinanceiraId) {
        this.categoriaFinanceiraId = categoriaFinanceiraId;
    }

    public Integer getCaixaId() {
        return caixaId;
    }

    public void setCaixaId(Integer caixaId) {
        this.caixaId = caixaId;
    }
}