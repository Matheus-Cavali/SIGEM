package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Evento {

    private int id;
    private String nome;
    private String descricao;
    private LocalDate dataRegistro;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private String status;
    private BigDecimal resultadoFinanceiro;
    private String observacoesHistorico;
    private int localId;
    private int categoriaEventoId;
    private int colaboradorCadastrouId;
    private int coordenadorId;

    public Evento() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getResultadoFinanceiro() {
        return resultadoFinanceiro;
    }

    public void setResultadoFinanceiro(BigDecimal resultadoFinanceiro) {
        this.resultadoFinanceiro = resultadoFinanceiro;
    }

    public String getObservacoesHistorico() {
        return observacoesHistorico;
    }

    public void setObservacoesHistorico(String observacoesHistorico) {
        this.observacoesHistorico = observacoesHistorico;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public int getCategoriaEventoId() {
        return categoriaEventoId;
    }

    public void setCategoriaEventoId(int categoriaEventoId) {
        this.categoriaEventoId = categoriaEventoId;
    }

    public int getColaboradorCadastrouId() {
        return colaboradorCadastrouId;
    }

    public void setColaboradorCadastrouId(int colaboradorCadastrouId) {
        this.colaboradorCadastrouId = colaboradorCadastrouId;
    }

    public int getCoordenadorId() {
        return coordenadorId;
    }

    public void setCoordenadorId(int coordenadorId) {
        this.coordenadorId = coordenadorId;
    }
}