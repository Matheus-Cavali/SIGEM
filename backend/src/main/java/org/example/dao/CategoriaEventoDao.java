package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.CategoriaEvento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaEventoDao {

    public boolean cadastrar(Connection conn, CategoriaEvento ce){
        String sql = "INSERT INTO categoria_evento (nome) VALUES (?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, ce.getNome());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar categoria de evento", e);
        }
    }

    public boolean atualizar(Connection conn, CategoriaEvento ce){
        String sql = "UPDATE categoria_evento SET nome = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, ce.getNome());
            stmt.setInt(2, ce.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar categoria de evento", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM categoria_evento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir categoria de evento: há registros vinculados.", e);

            throw new DatabaseException("Erro ao excluir categoria de evento", e);
        }
    }

    public CategoriaEvento buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM categoria_evento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar categoria de evento", e);
        }

        return null;
    }

    public CategoriaEvento buscarPorNomeExato(Connection conn, String nome){
        String sql = "SELECT * FROM categoria_evento WHERE UPPER(nome) = UPPER(?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, nome.trim());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao validar existência de categoria de evento", e);
        }

        return null;
    }

    public List<CategoriaEvento> listarTodos(Connection conn){
        String sql = "SELECT * FROM categoria_evento ORDER BY nome";
        List<CategoriaEvento> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar categorias de evento", e);
        }

        return lista;
    }

    public List<CategoriaEvento> listar(Connection conn, String nome){
        StringBuilder sql = new StringBuilder("SELECT * FROM categoria_evento WHERE 1=1");

        if(nome != null && !nome.trim().isEmpty())
            sql.append(" AND nome ILIKE ?");

        sql.append(" ORDER BY nome");

        List<CategoriaEvento> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())){

            if(nome != null && !nome.trim().isEmpty())
                stmt.setString(1, "%" + nome.trim() + "%");

            try(ResultSet rs = stmt.executeQuery()){
                while(rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar categorias de evento", e);
        }

        return lista;
    }

    private CategoriaEvento extrair(ResultSet rs) throws SQLException{
        return new CategoriaEvento(
                rs.getInt("id"),
                rs.getString("nome")
        );
    }
}