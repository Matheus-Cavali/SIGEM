package org.example.model;

import org.example.dao.ColaboradorDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class Colaborador extends Usuario {
    private LocalDate dataAdmissao;
    private LocalDate dataDemissao;

    public Colaborador() {
        super();
    }

    public static void cadastrar(Connection conn, int usuarioId, String data) throws SQLException {
        ColaboradorDao dao = new ColaboradorDao();
        dao.inserir(conn, usuarioId, data);
    }

    public static Colaborador buscarPorId(Connection conn, int usuarioId) throws SQLException {
        ColaboradorDao dao = new ColaboradorDao();
        return dao.buscarPorId(conn, usuarioId);
    }

    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }
    public LocalDate getDataDemissao() { return dataDemissao; }
    public void setDataDemissao(LocalDate dataDemissao) { this.dataDemissao = dataDemissao; }
}
