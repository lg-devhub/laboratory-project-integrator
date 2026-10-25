package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAO {
    // A comparacao do hash anterior impede duas trocas concorrentes de se sobrescreverem.
    public boolean alterarSenha(long id, String hashAnterior, String novoHash) throws SQLException {
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement(
                "UPDATE public.usuarios SET senha_hash=? WHERE id=? AND ativo AND senha_hash=?")) {
            statement.setString(1, novoHash);
            statement.setLong(2, id);
            statement.setString(3, hashAnterior);
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<Usuario> buscarPorLogin(String login) throws SQLException {
        return consultar("lower(login) = lower(?)", login);
    }

    public Optional<Usuario> buscarPorId(long id) throws SQLException {
        return consultar("id = ?", id);
    }

    // O filtro vem apenas das constantes acima. Entradas do usuario sao parametros.
    private Optional<Usuario> consultar(String filtro, Object valor) throws SQLException {
        String sql = "SELECT id, nome, login, senha_hash, perfil, ativo FROM public.usuarios WHERE " + filtro;
        try (Connection connection = Connect.conexao();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, valor);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(
                        result.getLong("id"), result.getString("nome"), result.getString("login"),
                        result.getString("senha_hash"), result.getString("perfil"), result.getBoolean("ativo")
                ));
            }
        }
    }
}
