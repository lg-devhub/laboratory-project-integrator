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
        if ("23505".equals(erro.getSQLState())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensagem", "Ja existe um ambiente com esse nome."));
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
