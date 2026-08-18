package br.com.projeto.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseConnection {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final String URL = dotenv.get("DATABASE_URL");
    private static final String USER = dotenv.get("DATABASE_USER");
    private static final String PASSWORD = dotenv.get("DATABASE_PASSWORD");

    public static Connection conectar() throws SQLException {

        if (URL == null || URL.isBlank()) {
            throw new SQLException(
                "A variável DATABASE_URL não foi configurada."
            );
        }

        if (USER == null || USER.isBlank()) {
            throw new SQLException(
                "A variável DATABASE_USER não foi configurada."
            );
        }

        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException(
                "A variável DATABASE_PASSWORD não foi configurada."
            );
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println(
                    "Erro ao fechar conexão: " + e.getMessage()
                );
            }
        }
    }
}