package org.example.conexao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Conexao {

    public static Connection getConexao() throws SQLException {
        try {
            Properties props = new Properties();
            InputStream input = Conexao.class.getResourceAsStream("/properties/config.properties");

            if (input == null) {
                throw new RuntimeException("Arquivo config.properties não encontrado em resources/properties/");
            }

            props.load(input);
            input.close();

            String url = props.getProperty("DB_URL");
            String user = props.getProperty("DB_USER");
            String pass = props.getProperty("DB_PASS");

            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, user, pass);

        } catch (Exception e) {
            throw new SQLException("Erro na conexão: " + e.getMessage());
        }
    }
}