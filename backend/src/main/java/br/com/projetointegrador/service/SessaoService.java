package br.com.projetointegrador.service;

import br.com.projetointegrador.model.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Optional;

// Verificacao compartilhada pelas rotas que precisam de uma conta autenticada.
public class SessaoService {
    public static final String USUARIO_ID = "usuarioId";
    private final AutenticacaoService autenticacao = new AutenticacaoService();

    public Optional<UsuarioAutenticado> usuarioAtual(HttpServletRequest request) throws SQLException {
        var sessao = request.getSession(false);
        if (sessao == null || !(sessao.getAttribute(USUARIO_ID) instanceof Long id)) {
            return Optional.empty();
        }
        var usuario = autenticacao.buscarContaAtiva(id);
        if (usuario.isEmpty()) sessao.invalidate();
        return usuario;
    }
}
