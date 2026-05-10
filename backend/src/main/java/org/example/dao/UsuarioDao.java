package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.Usuario;
import org.example.util.Criptografia;

import java.sql.*;
import java.sql.Statement;

public class UsuarioDao {
    public void primeiroacesso(String nome, String email, String senha, String cpf, int nivel_acesso, String tipo_usuario, String data) {
        String sql = "insert into usuario (nome, email, senha, cpf, nivel_acesso, status_ativo, tipo_usuario, primeiro_acesso) values (?,?,?,?,?,?,?,?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senha);
            stmt.setString(4, cpf);
            stmt.setInt(5, nivel_acesso);
            stmt.setBoolean(6, true);
            stmt.setString(7, tipo_usuario);
            stmt.setBoolean(8, true);

            int linhasAfetadas = stmt.executeUpdate();
            System.out.println("Linhas afetadas no Usuario: " + linhasAfetadas);

            if (linhasAfetadas > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGerado = rs.getInt(1);
                        System.out.println("ID gerado pelo banco: " + idGerado);
                        System.out.println("Tipo de usuário recebido: [" + tipo_usuario + "]");

                        if ("colaborador".equalsIgnoreCase(tipo_usuario.trim())) {
                            System.out.println(">>> Entrando no bloco de inserção de COLABORADOR...");
                            ColaboradorDao cDao = new ColaboradorDao();
                            cDao.inserirColaborador(idGerado, data);
                        } else {
                            System.out.println("AVISO: O tipo_usuario não correspondeu a nenhum critério.");
                        }
                    }

                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir usuário: " + e.getMessage());
        }
    }

    public boolean cadastrarUsuario(String nome, String email, String senha, String cpf, int nivel_acesso, String tipo_usuario, String data) {

        String sql = "insert into usuario (nome, email, senha, cpf, nivel_acesso, status_ativo, tipo_usuario) values (?,?,?,?,?,?,?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senha);
            stmt.setString(4, cpf);
            stmt.setInt(5, nivel_acesso);
            stmt.setBoolean(6, true);
            stmt.setString(7, tipo_usuario);

            int linhasAfetadas = stmt.executeUpdate();
            System.out.println("Linhas afetadas no Usuario: " + linhasAfetadas);

            if (linhasAfetadas > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGerado = rs.getInt(1);
                        System.out.println("ID gerado pelo banco: " + idGerado);
                        System.out.println("Tipo de usuário recebido: [" + tipo_usuario + "]");

                        if ("colaborador".equalsIgnoreCase(tipo_usuario.trim())) {
                            System.out.println(">>> Entrando no bloco de inserção de COLABORADOR...");
                            ColaboradorDao cDao = new ColaboradorDao();
                            cDao.inserirColaborador(idGerado, data);
                        } else if ("voluntario".equalsIgnoreCase(tipo_usuario.trim())) {
                            System.out.println(">>> Entrando no bloco de inserção de VOLUNTÁRIO...");
                            VoluntarioDao vDao = new VoluntarioDao();
                            vDao.inserirVoluntario(idGerado, data);
                        } else {
                            System.out.println("AVISO: O tipo_usuario não correspondeu a nenhum critério.");
                        }
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir usuário: " + e.getMessage());
            return false;
        }
    }

    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT * FROM usuario WHERE email = ?";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usu = new Usuario();

                    usu.setId(rs.getInt("id"));
                    usu.setNome(rs.getString("nome"));
                    usu.setEmail(rs.getString("email"));
                    usu.setSenha(rs.getString("senha"));
                    usu.setTipoUsuario(rs.getString("tipo_usuario"));

                    return usu;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorCPF(String cpf) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE CPF = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usu = new Usuario();
                    usu.setId(rs.getInt("id"));
                    usu.setNome(rs.getString("nome"));
                    usu.setEmail(rs.getString("email"));
                    usu.setSenha(rs.getString("senha"));
                    usu.setTipoUsuario(rs.getString("tipo_usuario"));
                    return usu;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public boolean mudarSenha(String cpf, String novasenha) {
        String sql = "UPDATE usuario SET senha = ? primeiro_acesso = false WHERE CPF = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novasenha);
            stmt.setString(2, cpf);
            if (stmt.executeUpdate() == 1) {
                return true;
            }
        }
        catch (SQLException e) {
            System.err.println("Erro ao trocar senha: " + e.getMessage());
        }
        return false;
    }

    public void criarAdmin() {
        if (buscarPorEmail("admin@sigem.com") == null) {
            System.out.println("Criando Administrador padrão do sistema...");

            String senhaHash = Criptografia.hashSenha("123");

            primeiroacesso("Administrador", "admin@sigem.com", senhaHash, "00000000000", 1, "colaborador", "09052026");
        }
    }
}

