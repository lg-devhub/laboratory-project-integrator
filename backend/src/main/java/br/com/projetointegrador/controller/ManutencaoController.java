package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.ManutencaoDAO;
import br.com.projetointegrador.model.ManutencaoDados;
import br.com.projetointegrador.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/ambientes/{ambienteId}/itens/{itemId}/manutencoes")
public class ManutencaoController {
    private final SessaoService sessoes=new SessaoService();
    private final ManutencaoDAO manutencoes=new ManutencaoDAO();
    private long usuario(HttpServletRequest request) throws SQLException {
        return sessoes.usuarioAtual(request).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).id();
    }
    @GetMapping
    public Object listar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId) throws SQLException {
        return manutencoes.consultar(usuario(request),ambienteId,itemId,null)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @GetMapping("/{id}")
    public Object consultar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,@PathVariable long id) throws SQLException {
        var lista=manutencoes.consultar(usuario(request),ambienteId,itemId,id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return lista.stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @PostMapping(consumes="application/json")
    public ResponseEntity<?> criar(HttpServletRequest request,@PathVariable long ambienteId,@PathVariable long itemId,
                                   @RequestBody(required=false) ManutencaoDados dados) throws SQLException {
        long usuario=usuario(request);
        if(dados==null)throw new IllegalArgumentException("Informe os dados da manutencao.");
        var manutencao=manutencoes.criar(usuario,ambienteId,itemId,dados.validar())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.created(URI.create("/api/ambientes/"+ambienteId+"/itens/"+itemId+"/manutencoes/"+manutencao.id())).body(manutencao);
    }
}
