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

    public boolean alterarSenha(long id, String senhaAtual, String novaSenha, String confirmacao) throws SQLException {
        if (novaSenha == null || novaSenha.codePointCount(0, novaSenha.length()) < 8
                || !novaSenha.codePoints().anyMatch(Character::isLetter)
                || !novaSenha.codePoints().anyMatch(Character::isDigit))
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres, uma letra e um numero.");
        if (novaSenha.getBytes(StandardCharsets.UTF_8).length > 72 || novaSenha.indexOf('\0') >= 0)
            throw new IllegalArgumentException("A nova senha deve ter ate 72 bytes em UTF-8 e nao pode conter caractere nulo.");
        if (!novaSenha.equals(confirmacao))
            throw new IllegalArgumentException("A confirmacao da nova senha nao confere.");
        if (senhaAtual == null || senhaAtual.isBlank() || senhaAtual.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Informe a senha atual corretamente.");
        var usuario = usuarioDAO.buscarPorId(id).filter(Usuario::isAtivo);
        if (usuario.isEmpty() || !BCrypt.checkpw(senhaAtual, usuario.get().getSenhaHash())) return false;
        return usuarioDAO.alterarSenha(id, usuario.get().getSenhaHash(), BCrypt.hashpw(novaSenha, BCrypt.gensalt()));
    }

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
