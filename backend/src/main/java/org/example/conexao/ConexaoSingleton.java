package org.example.conexao;

import java.sql.Connection;
import java.sql.SQLException;

public class ConexaoSingleton {

    private static ConexaoSingleton instancia;

    private ConexaoSingleton() {}

    public static synchronized ConexaoSingleton getInstance() {
        if (instancia == null) {
            instancia = new ConexaoSingleton();
        }
        return instancia;
    }

    public Connection getConexao() throws SQLException {
        return Conexao.getConexao();
    }
}
