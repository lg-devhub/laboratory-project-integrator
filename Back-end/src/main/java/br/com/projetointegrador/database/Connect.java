package br.com.projetointegrador.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {

    private static final String URL = "jdbc:mysql://localhost:3306/laboratory";
    private static final String USUARIO = "root";
    private static final String SENHA = "";

    public static Connection conexao() {

        try {
            Connection connection = DriverManager.getConnection(
                    URL,
                    USUARIO,
                    SENHA
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