package org.example.dao;

import org.example.conexao.Conexao;
import org.example.model.Voluntario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class VoluntarioDao {
    public void inserirVoluntario(int id, String data) {
        String sql = "INSERT INTO voluntario (usuario_id, data_inicio) VALUES (?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("ddMMyyyy");
            LocalDate dataConvertida = LocalDate.parse(data, formato);
            stmt.setInt(1, id);
            stmt.setObject(2, dataConvertida);
            stmt.executeUpdate();
            System.out.println("Dados de voluntário vinculados com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao vincular voluntário: " + e.getMessage());
        }
    }
}
