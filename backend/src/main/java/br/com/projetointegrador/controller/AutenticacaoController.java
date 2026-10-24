package br.com.projetointegrador.controller;

import br.com.projetointegrador.service.SessaoService;
import br.com.projetointegrador.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    private static final String USUARIO_ID = SessaoService.USUARIO_ID;
    private final AutenticacaoService autenticacao = new AutenticacaoService();
    private final SessaoService sessoes = new SessaoService();

    public record LoginRequest(String login, String senha) { }

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<?> login(@RequestBody LoginRequest dados, HttpServletRequest request)
            throws SQLException {
        // Uma nova tentativa nunca reaproveita a identidade de uma sessao anterior.
        invalidarSessao(request);
        if (dados.login() == null || dados.login().isBlank()
                || dados.senha() == null || dados.senha().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Informe login e senha."));
        }

        var usuario = autenticacao.autenticar(dados.login(), dados.senha());
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "Login ou senha invalidos."));
        }
        // O servidor gera um novo identificador de sessao. O cliente nao escolhe o perfil.
        request.getSession(true).setAttribute(USUARIO_ID, usuario.get().id());
        return ResponseEntity.ok(usuario.get());
    }

    @GetMapping("/me")
    public ResponseEntity<?> contaAtual(HttpServletRequest request) throws SQLException {
        var usuario = sessoes.usuarioAtual(request);
        if (usuario.isEmpty()) return naoAutenticado();
        return ResponseEntity.ok(usuario.get());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        invalidarSessao(request);
        return ResponseEntity.noContent().build();
    }

    private void invalidarSessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
    }

    private ResponseEntity<Map<String, String>> naoAutenticado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensagem", "Autenticacao necessaria."));
    }
}
