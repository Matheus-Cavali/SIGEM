package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.CategoriaDespesa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDespesaDao {

    public int inserir(CategoriaDespesa categoria) {
        String sql = "INSERT INTO categoria_despesa (nome) VALUES (?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, categoria.getNome());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir categoria de despesa: " + e.getMessage());
        }
        return -1;
    }

    public CategoriaDespesa buscarPorId(int id) {
        String sql = "SELECT * FROM categoria_despesa WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar categoria de despesa: " + e.getMessage());
        }
        return null;
    }

    public CategoriaDespesa buscarPorNome(String nome) {
        String sql = "SELECT * FROM categoria_despesa WHERE LOWER(nome) = LOWER(?)";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar categoria de despesa por nome: " + e.getMessage());
        }
        return null;
    }

    public List<CategoriaDespesa> listar(String filtroNome) {
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_despesa WHERE 1=1");
        if (filtroNome != null && !filtroNome.isEmpty()) sql.append(" AND nome ILIKE ?");
        sql.append(" ORDER BY nome");

        List<CategoriaDespesa> lista = new ArrayList<>();
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (filtroNome != null && !filtroNome.isEmpty()) stmt.setString(1, "%" + filtroNome + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar categorias de despesa: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizar(CategoriaDespesa categoria) {
        String sql = "UPDATE categoria_despesa SET nome = ? WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categoria.getNome());
            stmt.setInt(2, categoria.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar categoria de despesa: " + e.getMessage());
            return false;
        }
    }

    public boolean deletar(int id) {
        String sql = "DELETE FROM categoria_despesa WHERE id = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar categoria de despesa: " + e.getMessage());
            return false;
        }
    }

    private CategoriaDespesa extrair(ResultSet rs) throws SQLException {
        CategoriaDespesa categoria = new CategoriaDespesa();
        categoria.setId(rs.getInt("id"));
        categoria.setNome(rs.getString("nome"));
        return categoria;
    }
}
