package org.example.dao;

import org.example.exception.DatabaseException;
import org.example.model.DoacaoMaterial;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DoacaoMaterialDao {

    public int inserirDoacao(Connection conn, DoacaoMaterial dm){
        String sql = "INSERT INTO doacao (data_doacao, colaborador_id) VALUES (?, ?) RETURNING id";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setObject(1, dm.getData());
            stmt.setInt(2, dm.getColaboradorId());

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next())
                    return rs.getInt("id");
                throw new DatabaseException("Erro ao cadastrar doacao: nenhum ID retornado");
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar doacao", e);
        }
    }

    public void inserirDoacaoMaterial(Connection conn, DoacaoMaterial dm){
        String sql = "INSERT INTO doacao_material (doacao_id, material_id, quantidade) VALUES (?, ?, ?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, dm.getId());
            stmt.setInt(2, dm.getMaterialId());
            stmt.setInt(3, dm.getQuantidade());
            stmt.executeUpdate();
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao cadastrar doacao de material", e);
        }
    }

    public List<DoacaoMaterial> listar(Connection conn, String materialNome, Integer categoriaId, LocalDate dataInicio, LocalDate dataFim){
        StringBuilder sql = new StringBuilder(
            "SELECT d.id, d.data_doacao, d.colaborador_id, u.nome AS colaborador_nome, " +
            "dm.material_id, m.nome AS material_nome, dm.quantidade, " +
            "m.categoria_material_id, cm.nome AS categoria_nome " +
            "FROM doacao d " +
            "JOIN doacao_material dm ON d.id = dm.doacao_id " +
            "JOIN colaborador c ON d.colaborador_id = c.usuario_id " +
            "JOIN usuario u ON c.usuario_id = u.id " +
            "JOIN material m ON dm.material_id = m.id " +
            "LEFT JOIN categoria_material cm ON m.categoria_material_id = cm.id " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if(materialNome != null && !materialNome.trim().isEmpty()){
            sql.append(" AND m.nome ILIKE ?");
            params.add("%" + materialNome.trim() + "%");
        }
        if(categoriaId != null){
            sql.append(" AND m.categoria_material_id = ?");
            params.add(categoriaId);
        }
        if(dataInicio != null){
            sql.append(" AND d.data_doacao >= ?");
            params.add(dataInicio);
        }
        if(dataFim != null){
            sql.append(" AND d.data_doacao <= ?");
            params.add(dataFim);
        }

        sql.append(" ORDER BY d.data_doacao DESC, d.id DESC");

        List<DoacaoMaterial> lista = new ArrayList<>();
        try(PreparedStatement stmt = conn.prepareStatement(sql.toString())){

            for(int i = 0; i < params.size(); i++){
                Object param = params.get(i);
                if(param instanceof String){
                    stmt.setString(i + 1, (String) param);
                } else if(param instanceof Integer){
                    stmt.setInt(i + 1, (Integer) param);
                } else if(param instanceof LocalDate){
                    stmt.setObject(i + 1, param);
                }
            }

            try(ResultSet rs = stmt.executeQuery()){
                while(rs.next())
                    lista.add(extrair(rs));
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao listar doacoes de material", e);
        }

        return lista;
    }

    public DoacaoMaterial buscarPorDoacaoId(Connection conn, int doacaoId){
        String sql = "SELECT d.id, d.data_doacao, d.colaborador_id, dm.material_id, dm.quantidade " +
                     "FROM doacao d " +
                     "JOIN doacao_material dm ON d.id = dm.doacao_id " +
                     "WHERE d.id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, doacaoId);

            try(ResultSet rs = stmt.executeQuery()){
                if(rs.next()){
                    DoacaoMaterial dm = new DoacaoMaterial();
                    dm.setId(rs.getInt("id"));
                    dm.setData(rs.getObject("data_doacao", LocalDate.class));
                    dm.setColaboradorId(rs.getInt("colaborador_id"));
                    dm.setMaterialId(rs.getInt("material_id"));
                    dm.setQuantidade(rs.getInt("quantidade"));
                    return dm;
                }
            }
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao buscar doacao de material", e);
        }

        return null;
    }

    public boolean atualizar(Connection conn, DoacaoMaterial dm){
        String sqlDoacao = "UPDATE doacao SET data_doacao = ?, colaborador_id = ? WHERE id = ?";
        String sqlDoacaoMaterial = "UPDATE doacao_material SET material_id = ?, quantidade = ? WHERE doacao_id = ?";

        try{
            try(PreparedStatement stmt = conn.prepareStatement(sqlDoacao)){
                stmt.setObject(1, dm.getData());
                stmt.setInt(2, dm.getColaboradorId());
                stmt.setInt(3, dm.getId());
                stmt.executeUpdate();
            }

            try(PreparedStatement stmt = conn.prepareStatement(sqlDoacaoMaterial)){
                stmt.setInt(1, dm.getMaterialId());
                stmt.setInt(2, dm.getQuantidade());
                stmt.setInt(3, dm.getId());
                stmt.executeUpdate();
            }

            return true;
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao atualizar doacao de material", e);
        }
    }

    public void excluirDoacaoMaterial(Connection conn, int doacaoId){
        String sql = "DELETE FROM doacao_material WHERE doacao_id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, doacaoId);
            stmt.executeUpdate();
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao excluir doacao de material", e);
        }
    }

    public void excluirDoacao(Connection conn, int doacaoId){
        String sql = "DELETE FROM doacao WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, doacaoId);
            stmt.executeUpdate();
        }
        catch (SQLException e){
            throw new DatabaseException("Erro ao excluir doacao", e);
        }
    }

    private DoacaoMaterial extrair(ResultSet rs) throws SQLException{
        return new DoacaoMaterial(
            rs.getInt("id"),
            rs.getObject("data_doacao", LocalDate.class),
            rs.getInt("colaborador_id"),
            rs.getString("colaborador_nome"),
            rs.getInt("material_id"),
            rs.getString("material_nome"),
            rs.getInt("quantidade")
        );
    }
}
