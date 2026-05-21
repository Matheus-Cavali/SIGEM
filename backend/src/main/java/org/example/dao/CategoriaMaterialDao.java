package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.CategoriaMaterial;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaMaterialDao {

    public boolean cadastrar(Connection conn, CategoriaMaterial cm){
        String sql = "INSERT INTO categoria_material (nome) VALUES (?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, cm);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar categoria de material", e);
        }
    }

    public boolean atualizar(Connection conn, CategoriaMaterial cm){
        String sql = "UPDATE categoria_material SET nome = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, cm.getNome());
            stmt.setInt(2, cm.getId());
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar categoria de material", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM categoria_material WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir categoria de material: há registros vinculados.", e);

            throw new DatabaseException("Erro ao excluir categoria de material", e);
        }
    }

    public CategoriaMaterial buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM categoria_material WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar categoria de material", e);
        }

        return null;
    }

    public CategoriaMaterial buscarPorNomeExato(Connection conn, String nome){
        String sql = "SELECT * FROM categoria_material WHERE UPPER(nome) = UPPER(?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, nome.trim());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao validar existencia de categoria de material", e);
        }

        return null;
    }

    public List<CategoriaMaterial> listarTodos(Connection conn){
        String sql = "SELECT * FROM categoria_material ORDER BY nome";
        List<CategoriaMaterial> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar categorias de material", e);
        }

        return lista;
    }

    public List<CategoriaMaterial> listar(Connection conn, String nome){
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_material WHERE 1=1");

        if(nome != null && !nome.trim().isEmpty())
            sql.append(" AND nome ILIKE ?");
        sql.append(" ORDER BY nome");

        List<CategoriaMaterial> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if(nome != null && !nome.trim().isEmpty())
                stmt.setString(1, "%" + nome.trim() + "%");

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar categorias de material", e);
        }

        return lista;
    }

    private CategoriaMaterial extrair(ResultSet rs) throws SQLException{
        return new CategoriaMaterial(rs.getInt("id"),
                rs.getString("nome"));
    }

    private void preencherStatement(PreparedStatement stmt, CategoriaMaterial cm) throws SQLException{
        stmt.setString(1, cm.getNome());
    }
}
