package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.Endereco;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnderecoDao {

    public Integer cadastrar(Connection conn, Endereco e) {
        String sql = "INSERT INTO endereco (cep, logradouro, numero, complemento, bairro, cidade, uf) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencherStatement(stmt, e);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt("id");
            }
            return null;
        } catch (SQLException ex) {
            throw new DatabaseException("Erro ao cadastrar endereço", ex);
        }
    }

    public boolean atualizar(Connection conn, Endereco e) {
        String sql = "UPDATE endereco SET cep = ?, logradouro = ?, numero = ?, complemento = ?, " +
                "bairro = ?, cidade = ?, uf = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencherStatement(stmt, e);
            stmt.setInt(8, e.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DatabaseException("Erro ao atualizar endereço", ex);
        }
    }

    public boolean excluir(Connection conn, Integer id) {
        String sql = "DELETE FROM endereco WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DatabaseException("Erro ao excluir endereço", ex);
        }
    }

    public Endereco buscarPorId(Connection conn, Integer id) {
        String sql = "SELECT * FROM endereco WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return extrair(rs);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Erro ao buscar endereço", ex);
        }

        return null;
    }

    public List<Endereco> listarTodos(Connection conn) {
        String sql = "SELECT * FROM endereco ORDER BY id";
        List<Endereco> lista = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                lista.add(extrair(rs));
        } catch (SQLException ex) {
            throw new DatabaseException("Erro ao listar endereços", ex);
        }

        return lista;
    }

    private Endereco extrair(ResultSet rs) throws SQLException {
        Endereco e = new Endereco();
        e.setId(rs.getInt("id"));
        e.setCep(rs.getString("cep"));
        e.setLogradouro(rs.getString("logradouro"));
        e.setNumero(rs.getString("numero"));
        e.setComplemento(rs.getString("complemento"));
        e.setBairro(rs.getString("bairro"));
        e.setCidade(rs.getString("cidade"));
        e.setUf(rs.getString("uf"));
        return e;
    }

    private void preencherStatement(PreparedStatement stmt, Endereco e) throws SQLException {
        stmt.setString(1, e.getCep());
        stmt.setString(2, e.getLogradouro());
        stmt.setString(3, e.getNumero());
        stmt.setString(4, e.getComplemento());
        stmt.setString(5, e.getBairro());
        stmt.setString(6, e.getCidade());
        stmt.setString(7, e.getUf());
    }
}
