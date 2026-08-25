package br.com.projetointegrador;

import br.com.projetointegrador.database.Connect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {

    public static void main(String[] args) {

        Connection connection = Connect.conexao();

        if (connection == null) {
            System.out.println("Erro na conexão.");
            return;
        }

        String sql = "SELECT id, name, email FROM users";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                int id = result.getInt("id");
                String name = result.getString("name");
                String email = result.getString("email");

                System.out.println(
                    "ID: " + id +
                    " | Nome: " + name +
                    " | Email: " + email
                );
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            Connect.close(connection);
        }
    }
}
