package org.example.dao;

import org.example.model.RecursoSistema;
import org.example.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecursoSistemaDao {

    public List<RecursoSistema> listarTodos(Connection conn) throws SQLException {
        String sql = "SELECT * FROM recurso_sistema ORDER BY nome";
        List<RecursoSistema> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                RecursoSistema r = new RecursoSistema();
                r.setId(rs.getInt("id"));
                r.setNome(rs.getString("nome"));
                r.setDescricao(rs.getString("descricao"));
                lista.add(r);
            }
        }
        return lista;
    }

    public List<RecursoSistema> listarPorUsuario(Connection conn, int usuarioId) throws SQLException {
        String sql = "SELECT r.* FROM recurso_sistema r JOIN usuario_recurso ur ON r.id = ur.recurso_id WHERE ur.usuario_id = ? ORDER BY r.nome";
        List<RecursoSistema> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RecursoSistema r = new RecursoSistema();
                    r.setId(rs.getInt("id"));
                    r.setNome(rs.getString("nome"));
                    r.setDescricao(rs.getString("descricao"));
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    public List<Usuario> listarUsuariosPorPermissao(Connection conn, String permissaoNome) throws SQLException {
        String sql = "SELECT u.* FROM usuario u WHERE u.status_ativo = true AND (u.nivel_acesso = 1 OR u.id IN (SELECT ur.usuario_id FROM usuario_recurso ur JOIN recurso_sistema r ON r.id = ur.recurso_id WHERE r.nome = ?)) ORDER BY u.nome";
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, permissaoNome);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setNome(rs.getString("nome"));
                    u.setEmail(rs.getString("email"));
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
                    lista.add(u);
                }
            }
        }
        return lista;
    }

    public boolean atualizarPermissoes(Connection conn, int usuarioId, List<Integer> recursoIds) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM usuario_recurso WHERE usuario_id = ?")) {
            del.setInt(1, usuarioId);
            del.executeUpdate();
        }
        try (PreparedStatement ins = conn.prepareStatement("INSERT INTO usuario_recurso (usuario_id, recurso_id) VALUES (?, ?)")) {
            for (int rid : recursoIds) {
                ins.setInt(1, usuarioId);
                ins.setInt(2, rid);
                ins.addBatch();
            }
            ins.executeBatch();
        }
        return true;
    }
}
