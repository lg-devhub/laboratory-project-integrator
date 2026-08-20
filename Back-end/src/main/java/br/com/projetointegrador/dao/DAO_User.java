package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.Model_User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAO_User{
    // Insert User
    public void insert(Model_User User){
        String sql = " INSERT INTO `User` (name, email, password) VALUES (?, ?, ?)";

        try(
            Connection conexao = Connect.conexao();
            PreparedStatement stmt = conexao.prepareStatement(sql)
        ) {
            stmt.setString(1, User.getName());
            stmt.setString(2, User.getEmail());
            stmt.setString(3, User.getPassword());

            stmt.executeUpdate();

            stmt.close();
            Connect.close(conexao);
            System.out.println("Usuario cadastrado com sucesso");
        } catch(Exception e) {
            e.printStackTrace();
        }
    
    }
    
    // Select pelo email
    public Model_User selectByEmail(String email){
        String sql = " SELECT id, name, email, password FROM `User` WHERE email = ?";

        try(
            Connection conexao = Connect.conexao();
            PreparedStatement stmt = conexao.prepareStatement(sql);
        ) {
            stmt.setString(1,email);
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
}

