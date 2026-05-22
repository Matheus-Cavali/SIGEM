package org.example.dao;

import org.example.model.Documento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DocumentoDao {

    public int inserir(Connection conn, Documento documento) throws SQLException {
        String sql = "INSERT INTO documento (titulo, caminho_arquivo, data_documento, categoria_documento_id, usuario_titular_id, colaborador_lancou_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherStatement(stmt, documento);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Documento buscarPorId(Connection conn, int id) throws SQLException {
        String sql = sqlBase() + " WHERE d.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        }
        return null;
    }

    public List<Documento> listar(Connection conn, String filtroTitulo, Integer categoriaId, Integer usuarioTitularId) throws SQLException {
        StringBuilder sql = new StringBuilder(sqlBase() + " WHERE 1=1");
        if (filtroTitulo != null && !filtroTitulo.isEmpty()) sql.append(" AND d.titulo ILIKE ?");
        if (categoriaId != null) sql.append(" AND d.categoria_documento_id = ?");
        if (usuarioTitularId != null) sql.append(" AND d.usuario_titular_id = ?");
        sql.append(" ORDER BY d.data_documento DESC, d.id DESC");

        List<Documento> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (filtroTitulo != null && !filtroTitulo.isEmpty()) stmt.setString(idx++, "%" + filtroTitulo + "%");
            if (categoriaId != null) stmt.setInt(idx++, categoriaId);
            if (usuarioTitularId != null) stmt.setInt(idx++, usuarioTitularId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        }
        return lista;
    }

    public boolean atualizar(Connection conn, Documento documento) throws SQLException {
        String sql = "UPDATE documento SET titulo = ?, caminho_arquivo = ?, data_documento = ?, categoria_documento_id = ?, usuario_titular_id = ?, colaborador_lancou_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            preencherStatement(stmt, documento);
            stmt.setInt(7, documento.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM documento WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    private void preencherStatement(PreparedStatement stmt, Documento documento) throws SQLException {
        stmt.setString(1, documento.getTitulo());
        stmt.setString(2, documento.getCaminhoArquivo());
        stmt.setObject(3, documento.getDataDocumento());
        if (documento.getCategoriaDocumentoId() != null) stmt.setInt(4, documento.getCategoriaDocumentoId());
        else stmt.setNull(4, Types.INTEGER);
        if (documento.getUsuarioTitularId() != null) stmt.setInt(5, documento.getUsuarioTitularId());
        else stmt.setNull(5, Types.INTEGER);
        if (documento.getColaboradorLancouId() != null) stmt.setInt(6, documento.getColaboradorLancouId());
        else stmt.setNull(6, Types.INTEGER);
    }

    private String sqlBase() {
        return "SELECT d.*, c.nome AS categoria_nome, ut.nome AS usuario_titular_nome, cl.nome AS colaborador_lancou_nome " +
                "FROM documento d " +
                "LEFT JOIN categoria_documento c ON c.id = d.categoria_documento_id " +
                "LEFT JOIN usuario ut ON ut.id = d.usuario_titular_id " +
                "LEFT JOIN usuario cl ON cl.id = d.colaborador_lancou_id";
    }

    private Documento extrair(ResultSet rs) throws SQLException {
        Documento documento = new Documento();
        documento.setId(rs.getInt("id"));
        documento.setTitulo(rs.getString("titulo"));
        documento.setCaminhoArquivo(rs.getString("caminho_arquivo"));
        documento.setDataDocumento(rs.getObject("data_documento", LocalDate.class));
        documento.setCategoriaDocumentoId((Integer) rs.getObject("categoria_documento_id"));
        documento.setCategoriaDocumentoNome(rs.getString("categoria_nome"));
        documento.setUsuarioTitularId((Integer) rs.getObject("usuario_titular_id"));
        documento.setUsuarioTitularNome(rs.getString("usuario_titular_nome"));
        documento.setColaboradorLancouId((Integer) rs.getObject("colaborador_lancou_id"));
        documento.setColaboradorLancouNome(rs.getString("colaborador_lancou_nome"));
        return documento;
    }
}
