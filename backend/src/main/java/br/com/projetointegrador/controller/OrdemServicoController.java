package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.OrdemServicoDAO;
import br.com.projetointegrador.model.OrdemServicoDados;
import br.com.projetointegrador.model.OrdemServicoEdicao;
import br.com.projetointegrador.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/ambientes/{ambienteId}/itens/{itemId}/ordens-servico")
public class OrdemServicoController {
    private final SessaoService sessoes=new SessaoService();
    private final OrdemServicoDAO ordens=new OrdemServicoDAO();

    private long usuario(HttpServletRequest request) throws SQLException {
        return sessoes.usuarioAtual(request).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).id();
    }
    @GetMapping
    public Object listar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId) throws SQLException {
        return ordens.consultar(usuario(request),ambienteId,itemId,null)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @GetMapping("/{ordemId}")
    public Object consultar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,@PathVariable long ordemId) throws SQLException {
        var lista=ordens.consultar(usuario(request),ambienteId,itemId,ordemId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return lista.stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @PostMapping(consumes="application/json")
    public ResponseEntity<?> abrir(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,
                                   @RequestBody(required=false) OrdemServicoDados dados) throws SQLException {
        long usuario=usuario(request);
        if(dados==null)throw new IllegalArgumentException("Informe o problema e a prioridade.");
        var ordem=ordens.criar(usuario,ambienteId,itemId,dados.validar())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.created(URI.create("/api/ambientes/"+ambienteId+"/itens/"+itemId+"/ordens-servico/"+ordem.id())).body(ordem);
    }

    @PutMapping(path="/{ordemId}",consumes="application/json")
    public ResponseEntity<?> atualizar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,
                                        @PathVariable long ordemId,@RequestBody(required=false) OrdemServicoEdicao dados) throws SQLException {
        long usuario=usuario(request);
        if(dados==null || dados.versao()==null || dados.versao()<=0)
            throw new IllegalArgumentException("Informe a versao da OS. Atualize a consulta e tente novamente.");
        try {
            var ordem=ordens.atualizar(usuario,ambienteId,itemId,ordemId,dados)
                    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            return ResponseEntity.ok(ordem);
        } catch(OrdemServicoDAO.Conflito conflito) {
            boolean concluida=conflito.atual.status().equals("CONCLUIDA");
            return ResponseEntity.status(409).body(Map.of("codigo",concluida?"OS_CONCLUIDA":"OS_ALTERADA",
                "mensagem",concluida?"Esta OS foi concluida e esta disponivel somente para consulta.":
                    "Esta OS foi alterada desde a abertura do formulario. Confira a versao atual antes de salvar.",
                "ordemAtual",conflito.atual));
        }
    }
}
