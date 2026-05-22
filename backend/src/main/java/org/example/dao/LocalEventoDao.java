package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.LocalEvento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LocalEventoDao {

    public boolean cadastrar(Connection conn, LocalEvento le){
        String sql = "INSERT INTO local_evento (nome, endereco) VALUES (?, ?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, le.getNome());
            stmt.setString(2, le.getEndereco());
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar local de evento", e);
        }
    }

    public boolean atualizar(Connection conn, LocalEvento le){
        String sql = "UPDATE local_evento SET nome = ?, endereco = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, le.getNome());
            stmt.setString(2, le.getEndereco());
            stmt.setInt(3, le.getId());
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar local de evento", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM local_evento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir local: há registros vinculados.", e);

            throw new DatabaseException("Erro ao excluir local de evento", e);
        }
    }

    public LocalEvento buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM local_evento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar local de evento", e);
        }

        return null;
    }

    public LocalEvento buscarPorNome(Connection conn, String nome){
        String sql = "SELECT * FROM local_evento WHERE UPPER(nome) = UPPER(?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, nome.trim());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao validar local de evento", e);
        }

        return null;
    }

    public List<LocalEvento> listar(Connection conn, String nome){
        StringBuilder sql = new StringBuilder("SELECT * FROM local_evento WHERE 1=1");

        if(nome != null && !nome.trim().isEmpty())
            sql.append(" AND nome ILIKE ?");

        sql.append(" ORDER BY nome");

        List<LocalEvento> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())){

            if(nome != null && !nome.trim().isEmpty())
                stmt.setString(1, "%" + nome.trim() + "%");

            try(ResultSet rs = stmt.executeQuery()){
                while(rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar locais de evento", e);
        }

        return lista;
    }

    private LocalEvento extrair(ResultSet rs) throws SQLException{
        return new LocalEvento(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("endereco")
        );
    }
}