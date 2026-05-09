package org.example.model;

import java.time.LocalDate;

public class Colaborador extends Usuario {
    private LocalDate dataAdmissao;
    private LocalDate dataDemissao;

    public Colaborador() {
        super();
    }

    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }

    public LocalDate getDataDemissao() { return dataDemissao; }
    public void setDataDemissao(LocalDate dataDemissao) { this.dataDemissao = dataDemissao; }
}