package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterialDao {
    public boolean cadastrar(Connection conn, Material m){
        String sql = "INSERT INTO material (nome, descricao, quantidade_estoque, categoria_material_id) VALUES (?, ?, ?, ?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, m);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar material", e);
        }
    }

    public boolean atualizar(Connection conn, Material m){
        String sql = "UPDATE material SET nome = ?, descricao = ?, quantidade_estoque = ?, categoria_material_id = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, m);
            stmt.setInt(5, m.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar material", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM material WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir material: há registros vinculados.", e);

            throw new DatabaseException("Erro ao excluir material", e);
        }
    }

    public Material buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM material WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar material", e);
        }

        return null;
    }

    public Material buscarPorNomeExato(Connection conn, String nome){
        String sql = "SELECT * FROM material WHERE UPPER(nome) = UPPER(?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, nome.trim());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao validar existencia de material", e);
        }

        return null;
    }

    public List<Material> listarTodos(Connection conn){
        String sql = "SELECT * FROM material ORDER BY nome";
        List<Material> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar materiais", e);
        }

        return lista;
    }

    public List<Material> listar(Connection conn, String nome, Integer categoriaId){
        StringBuilder sql = new StringBuilder("SELECT * FROM material WHERE 1=1");

        if(nome != null && !nome.trim().isEmpty())
            sql.append(" AND nome ILIKE ?");
        if(categoriaId != null)
            sql.append(" AND categoria_material_id = ?");
        sql.append(" ORDER BY nome");

        List<Material> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            if(nome != null && !nome.trim().isEmpty())
                stmt.setString(i++, "%" + nome.trim() + "%");
            if(categoriaId != null)
                stmt.setObject(i++, categoriaId);

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar materiais", e);
        }

        return lista;
    }

    private Material extrair(ResultSet rs) throws SQLException{
        return new Material(rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getInt("quantidade_estoque"),
                rs.getObject("categoria_material_id", Integer.class));
    }

    private void preencherStatement(PreparedStatement stmt, Material m) throws SQLException{
        stmt.setString(1, m.getNome());
        stmt.setString(2, m.getDescricao());
        stmt.setInt(3, m.getQuantidadeEstoque());
        stmt.setObject(4, m.getCategoriaMaterialId());
    }
}