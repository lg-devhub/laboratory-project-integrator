package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.AmbienteDAO;
import br.com.projetointegrador.model.AmbienteDados;
import br.com.projetointegrador.model.UsuarioAutenticado;
import br.com.projetointegrador.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ambientes")
public class AmbienteController {
    private final SessaoService sessoes = new SessaoService();
    private final AmbienteDAO ambientes = new AmbienteDAO();

    private UsuarioAutenticado exigirUsuario(HttpServletRequest request, boolean administrador) throws SQLException {
        var usuario = sessoes.usuarioAtual(request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (administrador && !"ADMINISTRADOR".equals(usuario.perfil()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario;
    }

    @GetMapping
    public ResponseEntity<?> listar(HttpServletRequest request) throws SQLException {
        var usuario = exigirUsuario(request, false);
        return ResponseEntity.ok(ambientes.listarAutorizados(usuario.id()));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> criar(HttpServletRequest request, @RequestBody(required = false) AmbienteDados dados) throws SQLException {
        exigirUsuario(request, true);
        if (dados == null) throw new IllegalArgumentException("Informe os dados do ambiente.");
        var ambiente = ambientes.criar(dados.validar());
        return ResponseEntity.created(URI.create("/api/ambientes/" + ambiente.id())).body(ambiente);
    }

    @PutMapping(path = "/{id}", consumes = "application/json")
    public ResponseEntity<?> editar(HttpServletRequest request, @PathVariable long id,
                                   @RequestBody(required = false) AmbienteDados dados) throws SQLException {
        exigirUsuario(request, true);
        if (dados == null) throw new IllegalArgumentException("Informe os dados do ambiente.");
        var ambiente = ambientes.editar(id, dados.validar());
        return ambiente.isPresent() ? ResponseEntity.ok(ambiente.get()) : inexistente();
    }

    @GetMapping("/{id}/acessos")
    public ResponseEntity<?> acessos(HttpServletRequest request, @PathVariable long id) throws SQLException {
        exigirUsuario(request, true);
        return ambientes.ativo(id) ? ResponseEntity.ok(ambientes.listarAcessos(id)) : inexistente();
    }

    public record Atribuicoes(List<Long> usuarios) { }

    @PutMapping(path = "/{id}/acessos", consumes = "application/json")
    public ResponseEntity<?> atribuir(HttpServletRequest request, @PathVariable long id,
                                     @RequestBody(required = false) Atribuicoes dados) throws SQLException {
        exigirUsuario(request, true);
        if (dados == null || dados.usuarios() == null || dados.usuarios().stream().anyMatch(x -> x == null || x <= 0))
            throw new IllegalArgumentException("Informe a lista de usuarios.");
        return ambientes.salvarAcessos(id, dados.usuarios()) ? ResponseEntity.noContent().build() : inexistente();
    }

    private ResponseEntity<?> inexistente() {
        return ResponseEntity.status(404).body(Map.of("mensagem", "Ambiente ativo nao encontrado."));
    }
}
