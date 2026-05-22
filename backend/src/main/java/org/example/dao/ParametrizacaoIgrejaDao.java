package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.ParametrizacaoIgreja;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParametrizacaoIgrejaDao {

    public boolean cadastrar(Connection conn, ParametrizacaoIgreja p){
        String sql = "INSERT INTO parametrizacao_igreja " +
                "(nome_fantasia, razao_social, cnpj, caminho_logo, cor_primaria, cor_secundaria, telefone, email, site, endereco_completo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, p);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar parametrização da igreja", e);
        }
    }

    public boolean atualizar(Connection conn, ParametrizacaoIgreja p){
        String sql = "UPDATE parametrizacao_igreja SET nome_fantasia = ?, razao_social = ?, cnpj = ?, caminho_logo = ?, cor_primaria = ?, " +
                "cor_secundaria = ?, telefone = ?, email = ?, site = ?, endereco_completo = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, p);
            stmt.setInt(11, p.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar parametrização da igreja", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM parametrizacao_igreja WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao excluir parametrização da igreja", e);
        }
    }

    public ParametrizacaoIgreja buscar(Connection conn){
        String sql = "SELECT * FROM parametrizacao_igreja ORDER BY id LIMIT 1";

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            if(rs.next())
                return extrair(rs);
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar parametrização da igreja", e);
        }

        return null;
    }

    public ParametrizacaoIgreja buscarPorId(Connection conn, Integer id){
        String sql = "SELECT * FROM parametrizacao_igreja WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar parametrização da igreja", e);
        }

        return null;
    }

    public List<ParametrizacaoIgreja> listarTodos(Connection conn){
        String sql = "SELECT * FROM parametrizacao_igreja ORDER BY id";
        List<ParametrizacaoIgreja> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar parametrizações da igreja", e);
        }

        return lista;
    }

    public List<ParametrizacaoIgreja> listar(Connection conn, String nomeFantasia){
        StringBuilder sql = new StringBuilder("SELECT * FROM parametrizacao_igreja WHERE 1=1");

        if(nomeFantasia != null && !nomeFantasia.trim().isEmpty())
            sql.append(" AND nome_fantasia ILIKE ?");
        sql.append(" ORDER BY id");

        List<ParametrizacaoIgreja> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if(nomeFantasia != null && !nomeFantasia.trim().isEmpty())
                stmt.setString(1, "%" + nomeFantasia.trim() + "%");

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar parametrizações da igreja", e);
        }

        return lista;
    }

    private ParametrizacaoIgreja extrair(ResultSet rs) throws SQLException{
        ParametrizacaoIgreja p = new ParametrizacaoIgreja();
        p.setId(rs.getInt("id"));
        p.setNomeFantasia(rs.getString("nome_fantasia"));
        p.setRazaoSocial(rs.getString("razao_social"));
        p.setCnpj(rs.getString("cnpj"));
        p.setCaminhoLogo(rs.getString("caminho_logo"));
        p.setCorPrimaria(rs.getString("cor_primaria"));
        p.setCorSecundaria(rs.getString("cor_secundaria"));
        p.setTelefone(rs.getString("telefone"));
        p.setEmail(rs.getString("email"));
        p.setSite(rs.getString("site"));
        p.setEndereco(rs.getString("endereco_completo"));
        return p;
    }

    private void preencherStatement(PreparedStatement stmt, ParametrizacaoIgreja p) throws SQLException{
        stmt.setString(1, p.getNomeFantasia());
        stmt.setString(2, p.getRazaoSocial());
        stmt.setString(3, p.getCnpj());
        stmt.setString(4, p.getCaminhoLogo());
        stmt.setString(5, p.getCorPrimaria());
        stmt.setString(6, p.getCorSecundaria());
        stmt.setString(7, p.getTelefone());
        stmt.setString(8, p.getEmail());
        stmt.setString(9, p.getSite());
        stmt.setString(10, p.getEndereco());
    }
}
