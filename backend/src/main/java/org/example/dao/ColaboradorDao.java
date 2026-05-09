package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.Colaborador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ColaboradorDao {
    public void inserirColaborador(int id, String data) {
        String sql = "INSERT INTO colaborador (usuario_id, data_admissao) VALUES (?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("ddMMyyyy");
            LocalDate dataConvertida = LocalDate.parse(data, formato);
            stmt.setInt(1, id);
            stmt.setObject(2, dataConvertida);
            stmt.executeUpdate();
            System.out.println("Dados de colaborador vinculados com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao vincular colaborador: " + e.getMessage());
        }
    }
}
