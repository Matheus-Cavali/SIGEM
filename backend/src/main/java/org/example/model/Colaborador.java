package org.example.model;

import org.example.dao.ColaboradorDao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public class Colaborador extends Usuario {
    private LocalDate dataAdmissao;
    private LocalDate dataDemissao;

    private static ColaboradorDao dao;

    public static synchronized ColaboradorDao getColaboradorDao() {
        if (dao == null) dao = new ColaboradorDao();
        return dao;
    }

    public Colaborador() {
        super();
    }

    public static void cadastrar(Connection conn, int usuarioId, String data) {
        getColaboradorDao().inserir(conn, usuarioId, data);
    }

    public static Colaborador buscarPorId(Connection conn, int usuarioId) {
        return getColaboradorDao().buscarPorId(conn, usuarioId);
    }

    public static List<Colaborador> listar(Connection conn, String nome, String email) {
        return getColaboradorDao().listar(conn, nome, email);
    }

    public static boolean atualizar(Connection conn, Colaborador c) {
        return getColaboradorDao().atualizar(conn, c);
    }

    public static boolean atualizarDataDemissao(Connection conn, int id, LocalDate data) {
        return getColaboradorDao().atualizarDataDemissao(conn, id, data);
    }

    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }
    public LocalDate getDataDemissao() { return dataDemissao; }
    public void setDataDemissao(LocalDate dataDemissao) { this.dataDemissao = dataDemissao; }
}
