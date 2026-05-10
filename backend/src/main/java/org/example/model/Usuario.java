package org.example.model;

public class Usuario {
    private int id;
    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String rg;
    private String celular;
    private String rua;
    private String bairro;
    private String cep;
    private String cidade;
    private String estado;
    private int nivelAcesso;
    private boolean statusAtivo;
    private String tipoUsuario;
    private String data;
    private boolean primeiroAcesso;

    public Usuario() {}

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(int nivelAcesso) { this.nivelAcesso = nivelAcesso; }

    public boolean isStatusAtivo() { return statusAtivo; }
    public void setStatusAtivo(boolean statusAtivo) { this.statusAtivo = statusAtivo; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isPrimeiroAcesso() { return primeiroAcesso; }

    public void setPrimeiroAcesso(boolean primeiroAcesso) { this.primeiroAcesso = primeiroAcesso; }
}