package org.example.model;

import com.google.gson.Gson;
import org.example.dao.UsuarioDao;
import org.example.util.CpfUtil;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static UsuarioDao dao;

    public static synchronized UsuarioDao getUsuarioDao() {
        if (dao == null) dao = new UsuarioDao();
        return dao;
    }

    public Usuario() {}

    public Map<String, String> validar() {
        Map<String, String> erros = new LinkedHashMap<>();
        if (nome == null || nome.trim().isEmpty())
            erros.put("nome", "Nome é obrigatório");
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$"))
            erros.put("email", "Email inválido");
        if (cpf == null || !CpfUtil.validar(cpf))
            erros.put("cpf", "CPF inválido");
        if (senha == null || senha.trim().length() < 4)
            erros.put("senha", "Senha deve ter no mínimo 4 caracteres");
        return erros;
    }

    public void cadastrar(Connection conn, Usuario u) {
        Map<String, String> erros = u.validar();
        Usuario porEmail = getUsuarioDao().buscarPorEmail(conn, u.getEmail());
        if (porEmail != null) erros.put("email", "Email já cadastrado");
        String cpfLimpo = u.getCpf() != null ? u.getCpf().replaceAll("[^0-9]", "") : "";
        Usuario porCpf = getUsuarioDao().buscarPorCPF(conn, cpfLimpo);
        if (porCpf != null) erros.put("cpf", "CPF já cadastrado");
        if (!erros.isEmpty())
            throw new RuntimeException(new Gson().toJson(Map.of("erros", erros)));
        int id = getUsuarioDao().inserir(conn, u.getNome(), u.getEmail(), u.getSenha(), cpfLimpo, u.getNivelAcesso(), u.getTipoUsuario());
        u.setId(id);
    }

    public void alterar(Connection conn, Usuario u) {
        if (u.getId() <= 0) throw new IllegalArgumentException("ID inválido.");
        if (!getUsuarioDao().atualizar(conn, u))
            throw new RuntimeException("Erro ao atualizar usuário");
    }

    public void excluir(Connection conn, int id) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        if (!getUsuarioDao().deletar(conn, id))
            throw new RuntimeException("Erro ao excluir usuário");
    }

    public void alterarStatus(Connection conn, int id, boolean ativo) {
        if (!getUsuarioDao().alterarStatus(conn, id, ativo))
            throw new RuntimeException("Erro ao alterar status do usuário");
    }

    public int contarColaboradorAcessoTotalAtivo(Connection conn) {
        return getUsuarioDao().contarColaboradorAcessoTotalAtivo(conn);
    }

    public List<Usuario> filtrar(Connection conn, String nome, String email, String tipo) {
        return getUsuarioDao().listarTodos(conn, nome, email, tipo);
    }

    public static Usuario buscarPorEmail(Connection conn, String email) {
        return getUsuarioDao().buscarPorEmail(conn, email);
    }

    public static Usuario buscarPorCPF(Connection conn, String cpf) {
        return getUsuarioDao().buscarPorCPF(conn, cpf);
    }

    public static Usuario buscarPorId(Connection conn, int id) {
        return getUsuarioDao().buscarPorId(conn, id);
    }

    public static boolean mudarSenha(Connection conn, String cpf, String novaSenha) {
        return getUsuarioDao().mudarSenha(conn, cpf, novaSenha);
    }

    public static int cadastrar(Connection conn, String nome, String email, String senhaHash, String cpf, int nivelAcesso, String tipoUsuario) {
        return getUsuarioDao().inserir(conn, nome, email, senhaHash, cpf, nivelAcesso, tipoUsuario);
    }

    public static List<Usuario> listarTodos(Connection conn, String nome, String email, String tipo) {
        return getUsuarioDao().listarTodos(conn, nome, email, tipo);
    }

    public static boolean atualizar(Connection conn, Usuario u) {
        return getUsuarioDao().atualizar(conn, u);
    }

    public static boolean deletar(Connection conn, int id) {
        return getUsuarioDao().deletar(conn, id);
    }

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
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public boolean isPrimeiroAcesso() { return primeiroAcesso; }
    public void setPrimeiroAcesso(boolean primeiroAcesso) { this.primeiroAcesso = primeiroAcesso; }
}
