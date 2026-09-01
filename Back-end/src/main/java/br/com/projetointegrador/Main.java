package br.com.projetointegrador;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.server.Server;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        Connection connection = Connect.conexao();

        if (connection == null) {
            System.err.println("Falha ao conectar ao banco de dados PostgreSQL. O servidor não será iniciado.");
            return;
        }

        Connect.close(connection);

        try {
            Server.startServer();
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o servidor HTTP:");
            e.printStackTrace();
        }
    }
}