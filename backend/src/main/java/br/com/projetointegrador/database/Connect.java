package br.com.projetointegrador.database;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {

    // Variaveis do sistema tem prioridade sobre o arquivo backend/.env.
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static Connection conexao() throws SQLException {
        String url = configuracaoObrigatoria("DB_URL");
        String usuario = configuracaoObrigatoria("DB_USER");
        String senha = configuracaoObrigatoria("DB_PASSWORD");

        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("DB_URL deve ser uma URL JDBC do PostgreSQL.");
        }

        // Quem abre a conexao deve fecha-la, preferencialmente com try-with-resources.
        return DriverManager.getConnection(url, usuario, senha);
    }

    private static String configuracaoObrigatoria(String nome) {
        String valor = dotenv.get(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Configure " + nome + " no ambiente ou no arquivo backend/.env.");
        }
        return valor;
    }

}
