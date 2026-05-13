package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.Usuario;
import org.example.util.Criptografia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public int cadastrarUsuario(String nome, String email, String senha, String cpf, int nivel_acesso, String tipo_usuario, String data) {

        if (buscarPorEmail(email) != null) return 1;
        if (buscarPorCPF(cpf) != null) return 2;

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
            stmt.setBoolean(8, false);

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGerado = rs.getInt(1);

                        if ("colaborador".equalsIgnoreCase(tipo_usuario.trim())) {
                            ColaboradorDao cDao = new ColaboradorDao();
                            cDao.inserirColaborador(idGerado, data);
                        } else if ("voluntario".equalsIgnoreCase(tipo_usuario.trim())) {
                            VoluntarioDao vDao = new VoluntarioDao();
                            vDao.inserirVoluntario(idGerado, data);
                        }
                    }
                }
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir usuário: " + e.getMessage());
            return -1;
        }
    }

    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT * FROM usuario WHERE email = ?";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorCPF(String cpf) {
        String sql = "SELECT * FROM usuario WHERE CPF = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }
        return null;
    }

    public boolean mudarSenha(String cpf, String novasenha) {
        String sql = "UPDATE usuario SET senha = ?, primeiro_acesso = false WHERE CPF = ?";
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

    private Usuario extrairUsuario(ResultSet rs) throws SQLException {
        Usuario usu = new Usuario();
        usu.setId(rs.getInt("id"));
        usu.setNome(rs.getString("nome"));
        usu.setEmail(rs.getString("email"));
        usu.setSenha(rs.getString("senha"));
        usu.setCpf(rs.getString("cpf"));
        usu.setRg(rs.getString("rg"));
        usu.setCelular(rs.getString("celular"));
        usu.setRua(rs.getString("rua"));
        usu.setBairro(rs.getString("bairro"));
        usu.setCep(rs.getString("cep"));
        usu.setCidade(rs.getString("cidade"));
        usu.setEstado(rs.getString("estado"));
        usu.setNivelAcesso(rs.getInt("nivel_acesso"));
        usu.setStatusAtivo(rs.getBoolean("status_ativo"));
        usu.setTipoUsuario(rs.getString("tipo_usuario"));
        usu.setPrimeiroAcesso(rs.getBoolean("primeiro_acesso"));
        return usu;
    }

    public Usuario buscarPorId(int id) {
        String sql = "SELECT * FROM usuario WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrairUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por ID: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listarTodos(String filtroNome, String filtroEmail, String filtroTipo) {
        StringBuilder sql = new StringBuilder("SELECT * FROM usuario WHERE 1=1");
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND nome ILIKE ?");
        if (filtroEmail != null && !filtroEmail.isEmpty()) sql.append(" AND email ILIKE ?");
        if (filtroTipo != null && !filtroTipo.isEmpty()) sql.append(" AND tipo_usuario = ?");
        sql.append(" ORDER BY nome");

        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(i++, "%" + filtroNome + "%");
            if (filtroEmail != null && !filtroEmail.isEmpty()) stmt.setString(i++, "%" + filtroEmail + "%");
            if (filtroTipo != null && !filtroTipo.isEmpty()) stmt.setString(i++, filtroTipo);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrairUsuario(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar usuários: " + e.getMessage());
        }
        return lista;
    }

    public boolean deletar(int id) {
        String sqlRecurso = "DELETE FROM usuario_recurso WHERE usuario_id = ?";
        String sqlVoluntario = "DELETE FROM voluntario WHERE usuario_id = ?";
        String sqlColaborador = "DELETE FROM colaborador WHERE usuario_id = ?";
        String sqlUsuario = "DELETE FROM usuario WHERE id = ?";
        try (Connection conn = Conexao.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sqlRecurso)) {
                stmt.setInt(1, id); stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlVoluntario)) {
                stmt.setInt(1, id); stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlColaborador)) {
                stmt.setInt(1, id); stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlUsuario)) {
                stmt.setInt(1, id);
                if (stmt.executeUpdate() == 1) {
                    conn.commit();
                    return true;
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("Erro ao deletar usuário: " + e.getMessage());
        }
        return false;
    }

    public boolean alterarStatus(int id, boolean ativo) {
        String sql = "UPDATE usuario SET status_ativo = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, ativo);
            stmt.setInt(2, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao alterar status: " + e.getMessage());
            return false;
        }
    }

    public int contarColaboradorAcessoTotalAtivo() {
        String sql = "SELECT COUNT(*) FROM usuario WHERE tipo_usuario = 'colaborador' AND nivel_acesso = 1 AND status_ativo = true";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erro ao contar admins ativos: " + e.getMessage());
        }
        return 0;
    }

    public void criarAdmin() {
        if (buscarPorEmail("admin@sigem.com") == null) {
            System.out.println("Criando Administrador padrão do sistema...");

            String senhaHash = Criptografia.hashSenha("123");

            primeiroacesso("Administrador", "admin@sigem.com", senhaHash, "00000000000", 1, "colaborador", "09052026");
        }
    }
}

