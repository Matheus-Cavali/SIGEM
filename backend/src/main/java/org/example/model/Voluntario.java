package org.example.model;

import org.example.dao.VoluntarioDao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

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

    public static void cadastrar(Connection conn, int usuarioId, String data) {
        getVoluntarioDao().inserir(conn, usuarioId, data);
    }

    public static Voluntario buscarPorId(Connection conn, int usuarioId) {
        return getVoluntarioDao().buscarPorId(conn, usuarioId);
    }

    public static List<Voluntario> listar(Connection conn, String nome, String email) {
        return getVoluntarioDao().listar(conn, nome, email);
    }

    public static boolean atualizar(Connection conn, Voluntario v) {
        return getVoluntarioDao().atualizar(conn, v);
    }

    public static boolean atualizarDataDesligamento(Connection conn, int id, LocalDate data) {
        return getVoluntarioDao().atualizarDataDesligamento(conn, id, data);
    }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataDesligamento() { return dataDesligamento; }
    public void setDataDesligamento(LocalDate dataDesligamento) { this.dataDesligamento = dataDesligamento; }
}
