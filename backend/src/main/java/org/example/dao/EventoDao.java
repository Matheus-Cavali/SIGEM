package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.Evento;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoDao {

    public boolean cadastrar(Connection conn, Evento evento){
        String sql = """
                INSERT INTO evento (
                    nome,
                    descricao,
                    data_registro,
                    data_inicio,
                    data_fim,
                    status,
                    resultado_financeiro,
                    observacoes_historico,
                    local_id,
                    categoria_evento_id,
                    colaborador_cadastrou_id,
                    coordenador_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            preencherStatement(stmt, evento);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar evento", e);
        }
    }

    public boolean atualizar(Connection conn, Evento evento){
        String sql = """
                UPDATE evento
                SET
                    nome = ?,
                    descricao = ?,
                    data_inicio = ?,
                    data_fim = ?,
                    local_id = ?,
                    categoria_evento_id = ?,
                    coordenador_id = ?
                WHERE id = ?
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, evento.getNome());
            stmt.setString(2, evento.getDescricao());

            stmt.setTimestamp(3, evento.getDataInicio());
            stmt.setTimestamp(4, evento.getDataFim());

            if(evento.getLocalId() != null)
                stmt.setInt(5, evento.getLocalId());
            else
                stmt.setNull(5, Types.INTEGER);

            if(evento.getCategoriaEventoId() != null)
                stmt.setInt(6, evento.getCategoriaEventoId());
            else
                stmt.setNull(6, Types.INTEGER);

            if(evento.getCoordenadorId() != null)
                stmt.setInt(7, evento.getCoordenadorId());
            else
                stmt.setNull(7, Types.INTEGER);

            stmt.setInt(8, evento.getId());

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar evento", e);
        }
    }

    public boolean excluir(Connection conn, Integer id){
        String sql = "DELETE FROM evento WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, id);
            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao excluir evento", e);
        }
    }

    public Evento buscarPorId(Connection conn, Integer id){
        String sql = """
                SELECT
                    e.*,
                    ce.nome AS categoria_nome,
                    le.nome AS local_nome,
                    u.nome AS coordenador_nome
                FROM evento e
                LEFT JOIN categoria_evento ce
                    ON ce.id = e.categoria_evento_id
                LEFT JOIN local_evento le
                    ON le.id = e.local_id
                LEFT JOIN usuario u
                    ON u.id = e.coordenador_id
                WHERE e.id = ?
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, id);

            try(ResultSet rs = stmt.executeQuery()){

                if(rs.next()){
                    return extrair(rs);
                }
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar evento", e);
        }

        return null;
    }

    public List<Evento> listar(Connection conn, String nome, String status){

        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.*,
                    ce.nome AS categoria_nome,
                    le.nome AS local_nome,
                    u.nome AS coordenador_nome
                FROM evento e
                LEFT JOIN categoria_evento ce
                    ON ce.id = e.categoria_evento_id
                LEFT JOIN local_evento le
                    ON le.id = e.local_id
                LEFT JOIN usuario u
                    ON u.id = e.coordenador_id
                WHERE 1=1
                """);

        if(nome != null && !nome.trim().isEmpty()){
            sql.append(" AND e.nome ILIKE ?");
        }

        if(status != null && !status.trim().isEmpty()){
            sql.append(" AND e.status = ?");
        }

        sql.append(" ORDER BY e.data_inicio DESC");

        List<Evento> lista = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())){

            int index = 1;

            if(nome != null && !nome.trim().isEmpty()){
                stmt.setString(index++, "%" + nome.trim() + "%");
            }

            if(status != null && !status.trim().isEmpty()){
                stmt.setString(index, status);
            }

            try(ResultSet rs = stmt.executeQuery()){

                while(rs.next()){
                    lista.add(extrair(rs));
                }
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar eventos", e);
        }

        return lista;
    }

    public boolean alterarStatus(Connection conn, Integer id, String status){

        String sql = "UPDATE evento SET status = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, status);
            stmt.setInt(2, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao alterar status do evento", e);
        }
    }

    public boolean encerrarEvento(Connection conn,
                                  Integer id,
                                  String observacoesHistorico,
                                  BigDecimal resultadoFinanceiro){

        String sql = """
                UPDATE evento
                SET
                    status = 'ENCERRADO',
                    observacoes_historico = ?,
                    resultado_financeiro = ?
                WHERE id = ?
                """;

        try(PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, observacoesHistorico);

            if(resultadoFinanceiro != null)
                stmt.setBigDecimal(2, resultadoFinanceiro);
            else
                stmt.setNull(2, Types.DECIMAL);

            stmt.setInt(3, id);

            return stmt.executeUpdate() == 1;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao encerrar evento", e);
        }
    }

    private Evento extrair(ResultSet rs) throws SQLException{

        Evento evento = new Evento();

        evento.setId(rs.getInt("id"));
        evento.setNome(rs.getString("nome"));
        evento.setDescricao(rs.getString("descricao"));

        evento.setDataRegistro(rs.getDate("data_registro"));

        evento.setDataInicio(rs.getTimestamp("data_inicio"));
        evento.setDataFim(rs.getTimestamp("data_fim"));

        evento.setStatus(rs.getString("status"));

        evento.setResultadoFinanceiro(rs.getBigDecimal("resultado_financeiro"));

        evento.setObservacoesHistorico(rs.getString("observacoes_historico"));

        Integer localId = rs.getObject("local_id", Integer.class);
        evento.setLocalId(localId);

        Integer categoriaId = rs.getObject("categoria_evento_id", Integer.class);
        evento.setCategoriaEventoId(categoriaId);

        Integer colaboradorId = rs.getObject("colaborador_cadastrou_id", Integer.class);
        evento.setColaboradorCadastrouId(colaboradorId);

        Integer coordenadorId = rs.getObject("coordenador_id", Integer.class);
        evento.setCoordenadorId(coordenadorId);

        return evento;
    }

    private void preencherStatement(PreparedStatement stmt,
                                    Evento evento) throws SQLException{

        stmt.setString(1, evento.getNome());
        stmt.setString(2, evento.getDescricao());

        stmt.setDate(3, evento.getDataRegistro());

        stmt.setTimestamp(4, evento.getDataInicio());
        stmt.setTimestamp(5, evento.getDataFim());

        stmt.setString(6, evento.getStatus());

        if(evento.getResultadoFinanceiro() != null)
            stmt.setBigDecimal(7, evento.getResultadoFinanceiro());
        else
            stmt.setNull(7, Types.DECIMAL);

        stmt.setString(8, evento.getObservacoesHistorico());

        if(evento.getLocalId() != null)
            stmt.setInt(9, evento.getLocalId());
        else
            stmt.setNull(9, Types.INTEGER);

        if(evento.getCategoriaEventoId() != null)
            stmt.setInt(10, evento.getCategoriaEventoId());
        else
            stmt.setNull(10, Types.INTEGER);

        if(evento.getColaboradorCadastrouId() != null)
            stmt.setInt(11, evento.getColaboradorCadastrouId());
        else
            stmt.setNull(11, Types.INTEGER);

        if(evento.getCoordenadorId() != null)
            stmt.setInt(12, evento.getCoordenadorId());
        else
            stmt.setNull(12, Types.INTEGER);
    }
}