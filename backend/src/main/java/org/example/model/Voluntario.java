package org.example.model;

import org.example.dao.VoluntarioDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class Voluntario extends Usuario {
    private LocalDate dataInicio;
    private LocalDate dataDesligamento;

    private static VoluntarioDao dao;

    public static synchronized VoluntarioDao getVoluntarioDao() {
        if (dao == null) dao = new VoluntarioDao();
        return dao;
    }

    public Voluntario() {
        super();
    }

    public static void cadastrar(Connection conn, int usuarioId, String data) throws SQLException {
        getVoluntarioDao().inserir(conn, usuarioId, data);
    }

    public static Voluntario buscarPorId(Connection conn, int usuarioId) throws SQLException {
        return getVoluntarioDao().buscarPorId(conn, usuarioId);
    }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataDesligamento() { return dataDesligamento; }
    public void setDataDesligamento(LocalDate dataDesligamento) { this.dataDesligamento = dataDesligamento; }
}