package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.CategoriaFinanceira;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaFinanceiraDao {

    public boolean cadastrar(Connection conn, CategoriaFinanceira cf){
        String sql = "INSERT INTO categoria_financeira (nome) VALUES (?)";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, cf);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar categoria financeira", e);
        }
    }

    public boolean atualizar(Connection conn, CategoriaFinanceira cf){
        String sql = "UPDATE categoria_financeira SET nome = ? WHERE id = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, cf.getNome());
            stmt.setInt(2, cf.getId());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar categoria financeira", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM categoria_financeira WHERE id = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir categoria financeira: há registros vinculados.", e);
            throw new DatabaseException("Erro ao excluir categoria financeira", e);
        }
    }

    public CategoriaFinanceira buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM categoria_financeira WHERE id = ?";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()) return extrair(rs);
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao buscar categoria financeira", e);
        }
        return null;
    }

    public CategoriaFinanceira buscarPorNomeExato(Connection conn, String nome){
        String sql = "SELECT * FROM categoria_financeira WHERE UPPER(nome) = UPPER(?)";
        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, nome.trim());
            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()) return extrair(rs);
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao validar existencia de categoria financeira", e);
        }
        return null;
    }

    public List<CategoriaFinanceira> listarTodos(Connection conn){
        String sql = "SELECT * FROM categoria_financeira ORDER BY nome";
        List<CategoriaFinanceira> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            while(rs.next()) lista.add(extrair(rs));
        } catch (SQLException e){
            throw new DatabaseException("Erro ao listar categorias financeiras", e);
        }
        return lista;
    }

    public List<CategoriaFinanceira> listar(Connection conn, String nome){
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_financeira WHERE 1=1");
        if(nome != null && !nome.trim().isEmpty())
            sql.append(" AND nome ILIKE ?");
        sql.append(" ORDER BY nome");
        List<CategoriaFinanceira> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if(nome != null && !nome.trim().isEmpty())
                stmt.setString(1, "%" + nome.trim() + "%");
            try(ResultSet rs = stmt.executeQuery()){
                while(rs.next()) lista.add(extrair(rs));
            }
        } catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar categorias financeiras", e);
        }
        return lista;
    }

    private CategoriaFinanceira extrair(ResultSet rs) throws SQLException{
        return new CategoriaFinanceira(rs.getInt("id"), rs.getString("nome"));
    }

    private void preencherStatement(PreparedStatement stmt, CategoriaFinanceira cf) throws SQLException{
        stmt.setString(1, cf.getNome());
    }
}
