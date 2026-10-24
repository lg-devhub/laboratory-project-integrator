package br.com.projetointegrador.service;

import br.com.projetointegrador.dao.UsuarioDAO;
import br.com.projetointegrador.model.Usuario;
import br.com.projetointegrador.model.UsuarioAutenticado;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;

public class AutenticacaoService {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Optional<UsuarioAutenticado> buscarContaAtiva(long id) throws SQLException {
        return usuarioDAO.buscarPorId(id).filter(Usuario::isAtivo)
                .map(usuario -> new UsuarioAutenticado(
                        usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil()));
    }
    public Optional<UsuarioAutenticado> autenticar(String login, String senha) throws SQLException {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            return Optional.empty();
        }
        // BCrypt utiliza ate 72 bytes. Recusar o excesso evita aceitar sufixos ignorados.
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Optional.empty();
        }

        Optional<Usuario> encontrado = usuarioDAO.buscarPorLogin(login.strip());
        if (encontrado.isEmpty() || !encontrado.get().isAtivo()) {
            return Optional.empty();
        }
        Usuario usuario = encontrado.get();

        boolean senhaCorreta;
        try {
            // A senha e comparada como recebida, sem remover espacos ou mudar letras.
            senhaCorreta = BCrypt.checkpw(senha, usuario.getSenhaHash());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Hash de senha invalido no banco de dados.");
        }
        if (!senhaCorreta) {
            return Optional.empty();
        }

        return Optional.of(new UsuarioAutenticado(
                usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil()
        ));
    }
}
