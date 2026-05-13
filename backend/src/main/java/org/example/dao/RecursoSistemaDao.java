package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.RecursoSistema;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecursoSistemaDao {

    public List<RecursoSistema> listarTodos() {
        String sql = "SELECT * FROM recurso_sistema ORDER BY nome";
        List<RecursoSistema> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                RecursoSistema r = new RecursoSistema();
                r.setId(rs.getInt("id"));
                r.setNome(rs.getString("nome"));
                r.setDescricao(rs.getString("descricao"));
                lista.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar recursos: " + e.getMessage());
        }
        return lista;
    }

    public List<RecursoSistema> listarPorUsuario(int usuarioId) {
        String sql = "SELECT r.* FROM recurso_sistema r JOIN usuario_recurso ur ON r.id = ur.recurso_id WHERE ur.usuario_id = ? ORDER BY r.nome";
        List<RecursoSistema> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            System.err.println("Erro ao listar recursos do usuário: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizarPermissoes(int usuarioId, List<Integer> recursoIds) {
        String deleteSql = "DELETE FROM usuario_recurso WHERE usuario_id = ?";
        String insertSql = "INSERT INTO usuario_recurso (usuario_id, recurso_id) VALUES (?, ?)";

        try (Connection conn = Conexao.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                del.setInt(1, usuarioId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                for (int rid : recursoIds) {
                    ins.setInt(1, usuarioId);
                    ins.setInt(2, rid);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar permissões: " + e.getMessage());
            return false;
        }
    }
}