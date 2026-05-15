package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.util.Data;

public class AporteInvestimento {
    private int id;
    private int investimentoFuturoId;
    private BigDecimal valorAporte;
    private LocalDate dataAporte;
    private Integer caixaId;
    private Integer colaboradorId;
    private String colaboradorNome;

    public AporteInvestimento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvestimentoFuturoId() { return investimentoFuturoId; }
    public void setInvestimentoFuturoId(int investimentoFuturoId) { this.investimentoFuturoId = investimentoFuturoId; }

    public BigDecimal getValorAporte() { return valorAporte; }
    public void setValorAporte(BigDecimal valorAporte) { this.valorAporte = valorAporte; }

    public LocalDate getDataAporte() { return dataAporte; }
    public void setDataAporte(LocalDate dataAporte) { this.dataAporte = dataAporte; }

    public Integer getCaixaId() { return caixaId; }
    public void setCaixaId(Integer caixaId) { this.caixaId = caixaId; }

    public Integer getColaboradorId() { return colaboradorId; }
    public void setColaboradorId(Integer colaboradorId) { this.colaboradorId = colaboradorId; }

    public String getColaboradorNome() { return colaboradorNome; }
    public void setColaboradorNome(String colaboradorNome) { this.colaboradorNome = colaboradorNome; }

    public String validar(String dataStr) {
        if (valorAporte == null || valorAporte.compareTo(BigDecimal.ZERO) <= 0) {
            return "Valor do aporte deve ser maior que zero";
        }
        if (dataStr == null || dataStr.trim().isEmpty()) {
            return "Data da transa\u00e7\u00e3o \u00e9 obrigat\u00f3ria";
        }
        if (Data.parseFlexivel(dataStr) == null) {
            return "Data inv\u00e1lida. Use o formato dd/mm/aaaa ou ddmmaaaa";
        }
        return null;
    }
}