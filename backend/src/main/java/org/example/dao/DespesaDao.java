package org.example.dao;

import org.example.model.Despesa;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DespesaDao {

    public int inserir(Connection conn, Despesa despesa) throws SQLException {
        String sql = "INSERT INTO despesa (descricao, valor, data_lancamento, data_vencimento, categoria_despesa_id, colaborador_lancou_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, despesa.getDescricao());
            stmt.setBigDecimal(2, despesa.getValor());
            stmt.setObject(3, despesa.getDataLancamento() != null ? despesa.getDataLancamento() : LocalDate.now());
            stmt.setObject(4, despesa.getDataVencimento());
            stmt.setInt(5, despesa.getCategoriaDespesaId());
            if (despesa.getColaboradorLancouId() != null) {
                stmt.setInt(6, despesa.getColaboradorLancouId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public Despesa buscarPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT d.*, t.nome AS tipo_nome FROM despesa d " +
                "LEFT JOIN categoria_despesa t ON t.id = d.categoria_despesa_id WHERE d.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return extrair(rs);
            }
        }
        return null;
    }

    public List<Despesa> listar(Connection conn, String filtroDescricao, Integer filtroTipoId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT d.*, t.nome AS tipo_nome FROM despesa d " +
                        "LEFT JOIN categoria_despesa t ON t.id = d.categoria_despesa_id WHERE 1=1"
        );
        if (filtroDescricao != null && !filtroDescricao.isEmpty()) sql.append(" AND d.descricao ILIKE ?");
        if (filtroTipoId != null) sql.append(" AND d.categoria_despesa_id = ?");
        sql.append(" ORDER BY d.data_vencimento, d.id");

        List<Despesa> lista = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (filtroDescricao != null && !filtroDescricao.isEmpty()) stmt.setString(idx++, "%" + filtroDescricao + "%");
            if (filtroTipoId != null) stmt.setInt(idx++, filtroTipoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(extrair(rs));
            }
        }
        return lista;
    }

    public boolean atualizar(Connection conn, Despesa despesa) throws SQLException {
        String sql = "UPDATE despesa SET descricao = ?, valor = ?, data_vencimento = ?, categoria_despesa_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, despesa.getDescricao());
            stmt.setBigDecimal(2, despesa.getValor());
            stmt.setObject(3, despesa.getDataVencimento());
            stmt.setInt(4, despesa.getCategoriaDespesaId());
            stmt.setInt(5, despesa.getId());
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletar(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM despesa WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
    }

    private Despesa extrair(ResultSet rs) throws SQLException {
        Despesa d = new Despesa();
        d.setId(rs.getInt("id"));
        d.setDescricao(rs.getString("descricao"));
        d.setValor(rs.getBigDecimal("valor"));
        d.setDataLancamento(rs.getObject("data_lancamento", LocalDate.class));
        d.setDataVencimento(rs.getObject("data_vencimento", LocalDate.class));
        d.setDataPagamento(rs.getObject("data_pagamento", LocalDate.class));
        d.setCategoriaDespesaId(rs.getInt("categoria_despesa_id"));
        d.setCategoriaDespesaNome(rs.getString("tipo_nome"));
        d.setColaboradorLancouId((Integer) rs.getObject("colaborador_lancou_id"));
        return d;
    }
}
