package br.com.projetointegrador.controller;

import br.com.projetointegrador.dao.AmbienteDAO;
import br.com.projetointegrador.model.SetorOpcao;
import br.com.projetointegrador.service.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.List;

@RestController
public class SetorResponsavelController {
    private final SessaoService sessoes=new SessaoService();
    private final AmbienteDAO ambientes=new AmbienteDAO();

    // Metadados para o formulario. Nao retorna atribuicoes, descricao ou inventario.
    @GetMapping("/api/itens/setores-responsaveis")
    public List<SetorOpcao> listar(HttpServletRequest request) throws SQLException {
        if (sessoes.usuarioAtual(request).isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ambientes.listarSetoresResponsaveis();
    }
}
