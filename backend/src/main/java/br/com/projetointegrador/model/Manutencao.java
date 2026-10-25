package br.com.projetointegrador.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Manutencao(long id,long equipamentoId,Long ordemServicoId,LocalDate dataRealizacao,
                         String descricaoServico,String responsavelTecnico,long usuarioId,
                         String usuarioNome,OffsetDateTime registradoEm) { }
