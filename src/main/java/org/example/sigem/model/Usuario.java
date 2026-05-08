package org.example.sigem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuario")
@Data
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String nome;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String senha;

    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String rg;

    @Column(length = 20)
    private String celular;

    @Column(length = 200)
    private String rua;

    @Column(length = 100)
    private String bairro;

    @Column(length = 10)
    private String cep;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column
    private Integer nivelAcesso;

    @Column
    private Boolean statusAtivo;

    @Column(length = 20)
    private String tipoUsuario;
}