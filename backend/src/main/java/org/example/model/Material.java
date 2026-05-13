package org.example.model;

public class Material {
    private Integer id;
    private String nome;
    private String descricao;
    private int quantidadeEstoque;
    private Integer categoriaMaterialId;

    public Material() {}

    public Material(Integer id, String nome, String descricao, int quantidadeEstoque, Integer categoriaMaterialId) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.quantidadeEstoque = quantidadeEstoque;
        this.categoriaMaterialId = categoriaMaterialId;
    }

    public Material(String nome, String descricao, int quantidadeEstoque, Integer categoriaMaterialId) {
        this(null, nome, descricao, quantidadeEstoque, categoriaMaterialId);
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public Integer getCategoriaMaterialId() {
        return categoriaMaterialId;
    }

    public void setCategoriaMaterialId(Integer categoriaMaterialId) {
        this.categoriaMaterialId = categoriaMaterialId;
    }
}