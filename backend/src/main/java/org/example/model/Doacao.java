package org.example.model;

import java.time.LocalDate;

public class Doacao {
    private Integer id;
    private LocalDate data;
    private Integer colaboradorId;

    public Doacao(){}

    public Doacao(Integer id, LocalDate data, Integer colaboradorId) {
        this.id = id;
        this.data = data;
        this.colaboradorId = colaboradorId;
    }

    public Doacao(LocalDate data, Integer colaboradorId) {
        this(null, data, colaboradorId);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getColaboradorId() {
        return colaboradorId;
    }

    public void setColaboradorId(Integer colaboradorId) {
        this.colaboradorId = colaboradorId;
    }
}