package br.com.projetointegrador.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;

@RestControllerAdvice
public class ApiErros {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> jsonInvalido() {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Corpo JSON invalido."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> dadosInvalidos(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", erro.getMessage()));
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<?> bancoIndisponivel(SQLException erro) {
        if (erro instanceof org.postgresql.util.PSQLException pg && pg.getServerErrorMessage()!=null) {
            String restricao = pg.getServerErrorMessage().getConstraint();
            if ("ambientes_vinculos_existentes".equals(restricao))
                return ResponseEntity.status(409).body(Map.of("codigo", "AMBIENTE_COM_VINCULOS", "mensagem",
                    "Este ambiente possui itens localizados nele ou equipamentos sob sua responsabilidade, inclusive excluidos logicamente. Trate esses vinculos antes de retira-lo de uso."));
            if ("ambientes_setor_responsavel".equals(restricao))
                return ResponseEntity.status(409).body(Map.of("codigo", "SETOR_RESPONSAVEL", "mensagem",
                    "Este setor e responsavel por equipamentos e nao pode ser convertido em sala."));
        }
        if (erro instanceof org.postgresql.util.PSQLException pg && pg.getServerErrorMessage()!=null
                && "usuarios_ultimo_admin_ativo".equals(pg.getServerErrorMessage().getConstraint())) {
            return ResponseEntity.status(409).body(Map.of("codigo","ULTIMO_ADMINISTRADOR","mensagem",
                "O ultimo administrador ativo nao pode ser desativado nem perder esse perfil."));
        }
        if ("23505".equals(erro.getSQLState())) {
            String mensagem="Ja existe um registro com esse identificador.";
            if (erro instanceof org.postgresql.util.PSQLException pg && pg.getServerErrorMessage()!=null) {
                String restricao=pg.getServerErrorMessage().getConstraint();
                if ("usuarios_login_unico".equals(restricao)) mensagem="Ja existe uma conta com esse e-mail/login, inclusive entre as desativadas.";
                if ("ambientes_nome_unico".equals(restricao)) mensagem="Ja existe um ambiente com esse nome.";
                if ("itens_patrimonio_unico".equals(restricao)) mensagem="Ja existe um equipamento com esse patrimonio, inclusive entre os registros excluidos.";
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensagem", mensagem));
        }
        System.err.println("Falha de banco na API. SQLState: " + erro.getSQLState());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("mensagem", "Nao foi possivel acessar o banco. Tente novamente."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> configuracaoInvalida() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensagem", "Falha interna ao processar a autenticacao."));
    }
}
