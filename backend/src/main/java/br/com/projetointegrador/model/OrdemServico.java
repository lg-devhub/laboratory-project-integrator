package br.com.projetointegrador.model;

import java.time.OffsetDateTime;

public record OrdemServico(long id, long equipamentoId, long solicitanteId, String solicitanteNome,
                           String problema, String prioridade, String status, OffsetDateTime abertaEm,
                           String descricaoAtendimento, OffsetDateTime fechadaEm, long versao) { }
