package org.example.conexao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Conexao {
    private static Conexao instancia;
    private Connection conexao;

    private Conexao(){
        try{
            Properties props = new Properties();
            InputStream input = Conexao.class.getResourceAsStream("/properties/config.properties");

            if(input == null)
                throw new RuntimeException("Arquivo config.properties não encontrado em resources/properties/");

            props.load(input);
            input.close();
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");
            Class.forName("org.postgresql.Driver");

            this.conexao = DriverManager.getConnection(url, user, pass);
        }
        catch (Exception e){
            throw new RuntimeException("Erro na inicialização da conexão: " + e.getMessage());
        }
    }

    private static boolean conexaoValida(Connection c) {
        try {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static synchronized Connection getConexao() {
        if(instancia == null || !conexaoValida(instancia.conexao))
            instancia = new Conexao();

        return instancia.conexao;
    }
}