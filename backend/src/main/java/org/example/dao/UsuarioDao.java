package org.example.dao;

import org.example.conexao.Conexao;
import org.example.exception.DatabaseException;
import org.example.model.Usuario;
import org.example.util.Criptografia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDao {

    public int inserir(Connection conn, String nome, String email, String senha, String cpf, int nivelAcesso, String tipoUsuario) {
        String sql = "INSERT INTO usuario (nome, email, senha, cpf, nivel_acesso, status_ativo, tipo_usuario, primeiro_acesso) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senha);
            stmt.setString(4, cpf);
            stmt.setInt(5, nivelAcesso);
            stmt.setBoolean(6, true);
            stmt.setString(7, tipoUsuario);
            stmt.setBoolean(8, false);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao inserir usuário", e);
        }
        return -1;
    }

    public Usuario buscarPorEmail(Connection conn, String email) {
        String sql = "SELECT * FROM usuario WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrairUsuario(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar usuário por email", e);
        }
        return null;
    }

    public Usuario buscarPorCPF(Connection conn, String cpf) {
        String sql = "SELECT * FROM usuario WHERE CPF = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrairUsuario(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar usuário por CPF", e);
        }
        return null;
    }

    public Usuario buscarPorId(Connection conn, int id) {
        String sql = "SELECT * FROM usuario WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrairUsuario(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar usuário por ID", e);
        }
        return null;
    }

    public List<Usuario> listarTodos(Connection conn, String filtroNome, String filtroEmail, String filtroTipo) {
        StringBuilder sql = new StringBuilder("SELECT * FROM usuario WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (filtroNome != null && !filtroNome.isEmpty()) { sql.append(" AND nome ILIKE ?"); params.add("%" + filtroNome + "%"); }
        if (filtroEmail != null && !filtroEmail.isEmpty()) { sql.append(" AND email ILIKE ?"); params.add("%" + filtroEmail + "%"); }
        if (filtroTipo != null && !filtroTipo.isEmpty()) { sql.append(" AND tipo_usuario = ?"); params.add(filtroTipo); }
        sql.append(" ORDER BY nome");

        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrairUsuario(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar usuários", e);
        }
        return lista;
    }

    public boolean mudarSenha(Connection conn, String cpf, String novaSenha) {
        String sql = "UPDATE usuario SET senha = ?, primeiro_acesso = false WHERE CPF = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novaSenha);
            stmt.setString(2, cpf);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao mudar senha", e);
        }
    }

    public boolean atualizar(Connection conn, Usuario u) {
        String sql = "UPDATE usuario SET nome=?, email=?, cpf=?, rg=?, celular=?, rua=?, bairro=?, cep=?, cidade=?, estado=?, nivel_acesso=?, tipo_usuario=? WHERE id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getEmail());
            stmt.setString(3, u.getCpf());
            stmt.setString(4, u.getRg());
            stmt.setString(5, u.getCelular());
            stmt.setString(6, u.getRua());
            stmt.setString(7, u.getBairro());
            stmt.setString(8, u.getCep());
            stmt.setString(9, u.getCidade());
            stmt.setString(10, u.getEstado());
            stmt.setInt(11, u.getNivelAcesso());
            stmt.setString(12, u.getTipoUsuario());
            stmt.setInt(13, u.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar usuário", e);
        }
    }

    public boolean alterarStatus(Connection conn, int id, boolean ativo) {
        String sql = "UPDATE usuario SET status_ativo = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, ativo);
            stmt.setInt(2, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao alterar status do usuário", e);
        }
    }

    public boolean deletar(Connection conn, int id) {
        try {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM usuario_recurso WHERE usuario_id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM voluntario WHERE usuario_id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM colaborador WHERE usuario_id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new DatabaseException("Erro ao excluir usuário: há registros vinculados.", e);
            }
            throw new DatabaseException("Erro ao excluir usuário", e);
        }
    }

    public int contarColaboradorAcessoTotalAtivo(Connection conn) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE tipo_usuario = 'colaborador' AND nivel_acesso = 1 AND status_ativo = true";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao contar colaboradores", e);
        }
        return 0;
    }

    public void criarAdmin() {
        try {
            Connection conn = Conexao.getConexao();
            if (buscarPorEmail(conn, "admin@sigem.com") == null) {
                System.out.println("Criando Administrador padrao do sistema...");
                String senhaHash = Criptografia.hashSenha("123");
                String sql = "INSERT INTO usuario (nome, email, senha, cpf, nivel_acesso, status_ativo, tipo_usuario, primeiro_acesso) VALUES (?,?,?,?,?,?,?,?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, "Administrador");
                    stmt.setString(2, "admin@sigem.com");
                    stmt.setString(3, senhaHash);
                    stmt.setString(4, "00000000000");
                    stmt.setInt(5, 1);
                    stmt.setBoolean(6, true);
                    stmt.setString(7, "colaborador");
                    stmt.setBoolean(8, true);
                    stmt.executeUpdate();
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy");
                            java.time.LocalDate data = java.time.LocalDate.parse("09052026", fmt);
                            try (PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO colaborador (usuario_id, data_admissao) VALUES (?, ?)")) {
                                stmt2.setInt(1, id);
                                stmt2.setObject(2, data);
                                stmt2.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao criar admin", e);
        }
    }

    private Usuario extrairUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setEmail(rs.getString("email"));
        u.setSenha(rs.getString("senha"));
        u.setCpf(rs.getString("cpf"));
        u.setRg(rs.getString("rg"));
        u.setCelular(rs.getString("celular"));
        u.setRua(rs.getString("rua"));
        u.setBairro(rs.getString("bairro"));
        u.setCep(rs.getString("cep"));
        u.setCidade(rs.getString("cidade"));
        u.setEstado(rs.getString("estado"));
        u.setNivelAcesso(rs.getInt("nivel_acesso"));
        u.setStatusAtivo(rs.getBoolean("status_ativo"));
        u.setTipoUsuario(rs.getString("tipo_usuario"));
        u.setPrimeiroAcesso(rs.getBoolean("primeiro_acesso"));
        return u;
    }
}