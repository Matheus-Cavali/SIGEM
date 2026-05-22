package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.Documento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DocumentoDao {

    public int cadastrar(Connection conn, Documento d){
        String sql = "INSERT INTO documento (titulo, caminho_arquivo, data_documento, categoria_documento_id, usuario_titular_id, colaborador_lancou_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            preencherStatement(stmt, d);
            stmt.executeUpdate();

            try(ResultSet rs = stmt.getGeneratedKeys()){
                if(rs.next())
                    return rs.getInt(1);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar documento", e);
        }

        return -1;
    }

    public boolean atualizar(Connection conn, Documento d){
        String sql = "UPDATE documento SET titulo = ?, caminho_arquivo = ?, data_documento = ?, categoria_documento_id = ?, usuario_titular_id = ?, colaborador_lancou_id = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, d);
            stmt.setInt(7, d.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar documento", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM documento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao excluir documento", e);
        }
    }

    public Documento buscarPorId(Connection conn, Integer id){
        String sql = sqlBase() + " WHERE d.id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){
                if (rs.next())
                    return extrair(rs);
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar documento", e);
        }

        return null;
    }

    public List<Documento> listarTodos(Connection conn){
        String sql = sqlBase() + " ORDER BY d.data_documento DESC, d.id DESC";
        List<Documento> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next())
                lista.add(extrair(rs));
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar documentos", e);
        }

        return lista;
    }

    public List<Documento> listar(Connection conn, String titulo, Integer categoriaDocumentoId){
        StringBuilder sql = new StringBuilder(sqlBase() + " WHERE 1=1");

        if(titulo != null && !titulo.trim().isEmpty())
            sql.append(" AND d.titulo ILIKE ?");
        if(categoriaDocumentoId != null)
            sql.append(" AND d.categoria_documento_id = ?");
        sql.append(" ORDER BY d.data_documento DESC, d.id DESC");

        List<Documento> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int i = 1;
            if(titulo != null && !titulo.trim().isEmpty())
                stmt.setString(i++, "%" + titulo.trim() + "%");
            if(categoriaDocumentoId != null)
                stmt.setObject(i++, categoriaDocumentoId);

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao filtrar documentos", e);
        }

        return lista;
    }

    private String sqlBase(){
        return "SELECT d.*, c.nome AS categoria_nome, ut.nome AS usuario_titular_nome, cl.nome AS colaborador_lancou_nome " +
                "FROM documento d " +
                "LEFT JOIN categoria_documento c ON c.id = d.categoria_documento_id " +
                "LEFT JOIN usuario ut ON ut.id = d.usuario_titular_id " +
                "LEFT JOIN usuario cl ON cl.id = d.colaborador_lancou_id";
    }

    private Documento extrair(ResultSet rs) throws SQLException{
        Documento d = new Documento();
        d.setId(rs.getInt("id"));
        d.setTitulo(rs.getString("titulo"));
        d.setCaminhoArquivo(rs.getString("caminho_arquivo"));
        d.setDataDocumento(rs.getObject("data_documento", LocalDate.class));
        d.setCategoriaDocumentoId(rs.getObject("categoria_documento_id", Integer.class));
        d.setCategoriaDocumentoNome(rs.getString("categoria_nome"));
        d.setUsuarioTitularId(rs.getObject("usuario_titular_id", Integer.class));
        d.setUsuarioTitularNome(rs.getString("usuario_titular_nome"));
        d.setColaboradorLancouId(rs.getObject("colaborador_lancou_id", Integer.class));
        d.setColaboradorLancouNome(rs.getString("colaborador_lancou_nome"));
        return d;
    }

    private void preencherStatement(PreparedStatement stmt, Documento d) throws SQLException{
        stmt.setString(1, d.getTitulo());
        stmt.setString(2, d.getCaminhoArquivo());
        stmt.setObject(3, d.getDataDocumento());
        stmt.setObject(4, d.getCategoriaDocumentoId());
        stmt.setObject(5, d.getUsuarioTitularId());
        stmt.setObject(6, d.getColaboradorLancouId());
    }
}
