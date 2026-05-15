package org.example.model;

public class CategoriaMaterial {
    private Integer id;
    private String nome;

    public CategoriaMaterial() {}

    public CategoriaMaterial(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public CategoriaMaterial(String nome) {
        this(null, nome);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}