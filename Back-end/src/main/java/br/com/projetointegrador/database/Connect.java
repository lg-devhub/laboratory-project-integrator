package br.com.projetointegrador.database;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    public static Connection conexao() {

        try {
            Connection connection = DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD
            );

            System.out.println("Conexão com o banco realizada com sucesso!");

            return connection;

        } catch (SQLException e) {

            System.err.println("Erro ao conectar com o banco:");
            e.printStackTrace();

            return null;
        }
    }

    public static void close(Connection connection) {

        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexão fechada.");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
