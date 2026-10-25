package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.AmbienteDAO;
import br.com.projetointegrador.dao.ItemDAO;
import br.com.projetointegrador.dao.OrdemServicoDAO;
import br.com.projetointegrador.model.Ambiente;
import br.com.projetointegrador.model.ItemDados;
import br.com.projetointegrador.model.ItemEdicao;
import br.com.projetointegrador.model.UsuarioAutenticado;
import br.com.projetointegrador.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/ambientes/{ambienteId}")
public class ItemController {
    private final SessaoService sessoes=new SessaoService();
    private final AmbienteDAO ambientes=new AmbienteDAO();
    private final ItemDAO itens=new ItemDAO();

    private UsuarioAutenticado usuario(HttpServletRequest request) throws SQLException {
        return sessoes.usuarioAtual(request).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Ambiente exigirAcesso(long usuarioId,long ambienteId) throws SQLException {
        // Mesma resposta para ambiente inexistente, inativo ou sem atribuicao: nao vaza seus dados.
        return ambientes.buscarAutorizado(usuarioId,ambienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public Ambiente ambiente(HttpServletRequest request,@PathVariable long ambienteId) throws SQLException {
        return exigirAcesso(usuario(request).id(),ambienteId);
    }

    private boolean administrador(UsuarioAutenticado conta){return "ADMINISTRADOR".equals(conta.perfil());}

    @GetMapping("/itens")
    public Object listar(HttpServletRequest request,@PathVariable long ambienteId,@RequestParam(defaultValue="false") boolean excluidos) throws SQLException {
        exigirAcesso(usuario(request).id(),ambienteId);
        if(excluidos && !administrador(usuario(request)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return itens.listar(ambienteId,excluidos);
    }

    @GetMapping("/resumo-os")
    public Object resumoOS(HttpServletRequest request,@PathVariable long ambienteId) throws SQLException {
        return new OrdemServicoDAO().resumirAmbiente(usuario(request).id(),ambienteId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping(path="/itens",consumes="application/json")
    public ResponseEntity<?> criar(HttpServletRequest request,@PathVariable long ambienteId,
                                   @RequestBody(required=false) ItemDados dados) throws SQLException {
        var usuario=usuario(request);
        exigirAcesso(usuario.id(),ambienteId);
        if (dados==null) throw new IllegalArgumentException("Informe os dados do item.");
        var item=itens.criar(ambienteId,usuario.id(),dados.validar());
        if (item.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.created(URI.create("/api/ambientes/"+ambienteId+"/itens/"+item.get().id())).body(item.get());
    }

    @GetMapping("/itens/{itemId}")
    public ResponseEntity<?> consultar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId) throws SQLException {
        exigirAcesso(usuario(request).id(),ambienteId);
        var item=itens.buscar(ambienteId,itemId,administrador(usuario(request)));
        return item.isPresent()?ResponseEntity.ok(item.get()):ResponseEntity.notFound().build();
    }

    @PutMapping(path="/itens/{itemId}",consumes="application/json")
    public ResponseEntity<?> editar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,
                                    @RequestBody(required=false) ItemEdicao dados) throws SQLException {
        var usuario=usuario(request);
        exigirAcesso(usuario.id(),ambienteId);
        if(dados==null || dados.versao()==null || dados.versao()<=0)
            throw new IllegalArgumentException("Informe a revisao do item. Atualize o inventario e tente novamente.");
        try {
            var item=itens.editar(ambienteId,itemId,usuario.id(),dados);
            return item.isPresent()?ResponseEntity.ok(item.get()):ResponseEntity.notFound().build();
        } catch(ItemDAO.ConflitoEdicao conflito) {
            return ResponseEntity.status(409).body(Map.of("codigo","ITEM_ALTERADO","mensagem",
                    "Este item foi alterado desde que voce abriu o formulario. Confira a versao atual antes de salvar.","itemAtual",conflito.atual));
        }
    }

    @GetMapping("/itens/{itemId}/historico")
    public ResponseEntity<?> historico(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId) throws SQLException {
        exigirAcesso(usuario(request).id(),ambienteId);
        var historico=itens.historico(ambienteId,itemId,administrador(usuario(request)));
        return historico.isEmpty() ? ResponseEntity.status(404).body(Map.of("mensagem","Item nao encontrado.")) : ResponseEntity.ok(historico);
    }

    @DeleteMapping("/itens/{itemId}")
    public ResponseEntity<?> excluir(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,
                                      @RequestParam long versao) throws SQLException {
        var conta=usuario(request);
        exigirAcesso(conta.id(),ambienteId);
        if(versao<=0)throw new IllegalArgumentException("Informe a versao atual do item.");
        try {
            var item=itens.excluir(ambienteId,itemId,conta.id(),versao)
                    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            return ResponseEntity.ok(item);
        }catch(ItemDAO.ExclusaoBloqueada erro) {
            return ResponseEntity.status(409).body(Map.of("codigo","OS_PENDENTE","mensagem",
                "Este equipamento possui OS aberta ou em andamento. Conclua o atendimento antes de excluir."));
        }catch(ItemDAO.ConflitoEdicao conflito) {
            return ResponseEntity.status(409).body(Map.of("codigo","ITEM_ALTERADO","mensagem",
                "O item foi alterado. Revise os dados atuais antes de confirmar a exclusao.","itemAtual",conflito.atual));
        }
    }

    @GetMapping("/itens/{itemId}/foto")
    public ResponseEntity<?> foto(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId) throws SQLException {
        exigirAcesso(usuario(request).id(),ambienteId);
        var foto=itens.foto(ambienteId,itemId,administrador(usuario(request)));
        return foto.isEmpty() ? ResponseEntity.notFound().build()
                : ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.get().tipo())).body(foto.get().bytes());
    }
}
