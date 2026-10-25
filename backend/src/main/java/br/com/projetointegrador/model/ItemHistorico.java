package br.com.projetointegrador.model;

import java.time.OffsetDateTime;

public record ItemHistorico(long id, String acao, long usuarioId, String usuarioNome, OffsetDateTime dataHora,
                           String statusAnterior, String statusNovo) { }
