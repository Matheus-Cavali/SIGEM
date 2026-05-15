package org.example.dao;

import org.example.conexao.Conexao;
import org.example.exception.DatabaseException;
import org.example.model.CategoriaMaterial;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaMaterialDao {
    private static final CategoriaMaterialDao instancia = new CategoriaMaterialDao();

    private CategoriaMaterialDao(){}

    public static CategoriaMaterialDao getInstancia(){
        return instancia;
    }

    public boolean cadastrar(CategoriaMaterial cm){
        String sql = "INSERT INTO categoria_material (nome) VALUES (?)";

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, cm.getNome());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar categoria de material", e);
        }
    }

    public boolean atualizar(CategoriaMaterial cm){
        String sql = "UPDATE categoria_material SET nome = ? WHERE id = ?";

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, cm.getNome());
            stmt.setInt(2, cm.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar categoria de material", e);
        }
    }

    public boolean excluir(Integer id){
        String sql = "DELETE FROM categoria_material WHERE id = ?";

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir categoria de material: há registros vinculados.", e);
            throw new DatabaseException("Erro ao excluir categoria de material", e);
        }
    }

    public CategoriaMaterial buscarPorId(Integer id){
        String sql = "SELECT * FROM categoria_material WHERE id = ?";

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, id);
            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next()) return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar categoria de material", e);
        }

        return null;
    }

    public CategoriaMaterial buscarPorNomeExato(String nome){
        String sql = "SELECT * FROM categoria_material WHERE UPPER(nome) = UPPER(?)";

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, nome.trim());
            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next()) return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao validar existencia de categoria de material", e);
        }

        return null;
    }

    public List<CategoriaMaterial> listarTodos(){
        String sql = "SELECT * FROM categoria_material ORDER BY nome";
        List<CategoriaMaterial> lista = new ArrayList<>();

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()){

            while (rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar categorias de material", e);
        }

        return lista;
    }

    public List<CategoriaMaterial> listar(String nome){
        String sql = "SELECT * FROM categoria_material WHERE nome ILIKE ? ORDER BY nome";
        List<CategoriaMaterial> lista = new ArrayList<>();

        try(Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)){

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
}