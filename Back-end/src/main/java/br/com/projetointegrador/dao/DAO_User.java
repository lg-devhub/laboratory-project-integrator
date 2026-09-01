package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.Model_User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAO_User {

    public void insert(Model_User user) {
        String sql = "INSERT INTO \"User\" (name, email, password) VALUES (?, ?, ?)";
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

        try (
            Connection conexao = Connect.conexao();
            PreparedStatement stmt = conexao.prepareStatement(sql)
        ) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, hashedPassword);

            stmt.executeUpdate();

            stmt.close();
            Connect.close(conexao);
            System.out.println("Usuario cadastrado com sucesso");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Select pelo email (Ajustada sintaxe PostgreSQL)
    public Model_User selectByEmail(String email) {
        String sql = "SELECT id, name, email, password FROM \"User\" WHERE email = ?";

        try (
            Connection conexao = Connect.conexao();
            PreparedStatement stmt = conexao.prepareStatement(sql)
        ) {
            stmt.setString(1, email);
            ResultSet resultado = stmt.executeQuery();

            if (resultado.next()) {
                return new Model_User(
                        resultado.getInt("id"),
                        resultado.getString("name"),
                        resultado.getString("email"),
                        resultado.getString("password")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Método Agregado: Autenticação comparando senha comum enviada com o Hash BCrypt do banco
    public Model_User authenticate(String email, String plainPassword) {
        Model_User user = selectByEmail(email);
        if (user != null && BCrypt.checkpw(plainPassword, user.getPassword())) {
            return user;
        }
        return null;
    }
}