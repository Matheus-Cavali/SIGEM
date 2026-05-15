package org.example.dao;

import org.example.conexao.Conexao;
import org.example.exception.DatabaseException;
import org.example.model.CategoriaEvento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaEventoDao {
    private static final CategoriaEventoDao instancia = new CategoriaEventoDao();

    private CategoriaEventoDao(){}

    public static CategoriaEventoDao getInstancia(){
        return instancia;
    }

    public boolean cadastrar(CategoriaEvento ce){
        String sql = "INSERT INTO categoria_evento (nome) VALUES (?)";

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, ce.getNome());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar categoria de evento", e);
        }
    }

    public boolean atualizar(CategoriaEvento ce){
        String sql = "UPDATE categoria_evento SET nome = ? WHERE id = ?";

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, ce.getNome());
            stmt.setInt(2, ce.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar categoria de evento", e);
        }
    }

    public boolean excluir(Integer id){
        String sql = "DELETE FROM categoria_evento WHERE id = ?";

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            if("23503".equals(e.getSQLState()))
                throw new DatabaseException("Erro ao excluir categoria de evento: há registros vinculados.", e);

            throw new DatabaseException("Erro ao excluir categoria de evento", e);
        }
    }

    public CategoriaEvento buscarPorId(Integer id){
        String sql = "SELECT * FROM categoria_evento WHERE id = ?";

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

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

    public CategoriaEvento buscarPorNomeExato(String nome){
        String sql = "SELECT * FROM categoria_evento WHERE UPPER(nome) = UPPER(?)";

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

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

    public List<CategoriaEvento> listarTodos(){
        String sql = "SELECT * FROM categoria_evento ORDER BY nome";
        List<CategoriaEvento> lista = new ArrayList<>();

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar categorias de evento", e);
        }

        return lista;
    }

    public List<CategoriaEvento> listar(String nome){
        String sql = "SELECT * FROM categoria_evento WHERE nome ILIKE ? ORDER BY nome";
        List<CategoriaEvento> lista = new ArrayList<>();

        try(Connection conn = Conexao.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)){

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