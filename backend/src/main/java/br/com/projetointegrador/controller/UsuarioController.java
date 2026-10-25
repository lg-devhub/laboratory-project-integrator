package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.GestaoUsuarioDAO;
import br.com.projetointegrador.model.UsuarioDados;
import br.com.projetointegrador.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final SessaoService sessoes=new SessaoService();
    private final GestaoUsuarioDAO contas=new GestaoUsuarioDAO();
    public record Desativacao(Long versao) { }
    private long admin(HttpServletRequest request) throws SQLException {
        var conta=sessoes.usuarioAtual(request).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!"ADMINISTRADOR".equals(conta.perfil()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return conta.id();
    }
    @GetMapping public Object listar(HttpServletRequest request) throws SQLException {return contas.listar(admin(request));}
    @PostMapping(consumes="application/json")
    public ResponseEntity<?> criar(HttpServletRequest request,@RequestBody(required=false) UsuarioDados dados) throws SQLException {
        long autor=admin(request);if(dados==null)throw new IllegalArgumentException("Informe os dados da conta.");
        var conta=contas.salvar(autor,null,dados.validar(true),false,null).orElseThrow();
        return ResponseEntity.created(URI.create("/api/usuarios")).body(conta);
    }
    @PutMapping(path="/{id}",consumes="application/json")
    public Object editar(HttpServletRequest request,@PathVariable long id,@RequestBody(required=false) UsuarioDados dados) throws SQLException {
        long autor=admin(request);if(dados==null)throw new IllegalArgumentException("Informe os dados da conta.");
        var validado=dados.validar(false);
        return contas.salvar(autor,id,validado,false,validado.versao()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @PostMapping(path="/{id}/desativacao",consumes="application/json")
    public Object desativar(HttpServletRequest request,@PathVariable long id,@RequestBody(required=false) Desativacao dados) throws SQLException {
        long autor=admin(request);if(dados==null || dados.versao()==null || dados.versao()<=0)throw new IllegalArgumentException("Informe a versao atual da conta.");
        return contas.salvar(autor,id,null,true,dados.versao()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @ExceptionHandler(GestaoUsuarioDAO.SemPermissao.class)
    public ResponseEntity<?> permissaoAlterada(){return ResponseEntity.status(403).body(Map.of("mensagem","Sua permissao administrativa mudou. Atualize a sessao."));}
    @ExceptionHandler(GestaoUsuarioDAO.Conflito.class)
    public ResponseEntity<?> conflito(GestaoUsuarioDAO.Conflito e){return ResponseEntity.status(409).body(Map.of("mensagem","A conta foi alterada. Atualize os dados antes de salvar.","codigo","USUARIO_ALTERADO","usuarioAtual",e.atual));}
}
