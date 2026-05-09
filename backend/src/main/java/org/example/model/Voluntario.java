package org.example.model;

import java.time.LocalDate;

public class Voluntario extends Usuario {
    private LocalDate dataInicio;
    private LocalDate dataDesligamento;

    public Voluntario() {
        super();
    }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataDesligamento() { return dataDesligamento; }
    public void setDataDesligamento(LocalDate dataDesligamento) { this.dataDesligamento = dataDesligamento; }
}