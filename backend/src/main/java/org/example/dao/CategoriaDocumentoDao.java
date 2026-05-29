package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.CategoriaDocumento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDocumentoDao {

    public boolean cadastrar(Connection conn, CategoriaDocumento cd){
        String sql = "INSERT INTO categoria_documento (nome) VALUES (?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, cd);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar categoria de documento", e);
        }
    }

    public boolean atualizar(Connection conn, CategoriaDocumento cd){
        String sql = "UPDATE categoria_documento SET nome = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, cd.getNome());
            stmt.setInt(2, cd.getId());
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar categoria de documento", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM categoria_documento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Não é possível excluir esta categoria porque existem documentos vinculados a ela.", e);

            throw new DatabaseException("Erro ao excluir categoria de documento", e);
        }
    }

    public CategoriaDocumento buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM categoria_documento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar categoria de documento", e);
        }

        return null;
    }

    public CategoriaDocumento buscarPorNomeExato(Connection conn, String nome){
        String sql = "SELECT * FROM categoria_documento WHERE UPPER(nome) = UPPER(?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, nome.trim());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao validar existencia de categoria de documento", e);
        }

        return null;
    }

    public List<CategoriaDocumento> listarTodos(Connection conn){
        String sql = "SELECT * FROM categoria_documento ORDER BY nome";
        List<CategoriaDocumento> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar categorias de documento", e);
        }

        return lista;
    }

    public List<CategoriaDocumento> listar(Connection conn, String nome){
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_documento WHERE 1=1");

        if(nome != null && !nome.trim().isEmpty())
            sql.append(" AND nome ILIKE ?");
        sql.append(" ORDER BY nome");

        List<CategoriaDocumento> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if(nome != null && !nome.trim().isEmpty())
                stmt.setString(1, "%" + nome.trim() + "%");

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar categorias de documento", e);
        }

        return lista;
    }

    private CategoriaDocumento extrair(ResultSet rs) throws SQLException{
        return new CategoriaDocumento(rs.getInt("id"),
                rs.getString("nome"));
    }

    private void preencherStatement(PreparedStatement stmt, CategoriaDocumento cd) throws SQLException{
        stmt.setString(1, cd.getNome());
    }
}
