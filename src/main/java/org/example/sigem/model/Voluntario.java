package org.example.sigem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@Table(name = "voluntario")
// Diz qual é o nome da coluna de ID que liga com a tabela pai
@PrimaryKeyJoinColumn(name = "usuario_id")
@Data
@EqualsAndHashCode(callSuper = true) // O Lombok pede isso quando usamos extends
public class Voluntario extends Usuario {

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_desligamento")
    private LocalDate dataDesligamento;
}