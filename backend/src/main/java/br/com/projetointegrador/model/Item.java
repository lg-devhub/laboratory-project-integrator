package br.com.projetointegrador.model;

import java.time.OffsetDateTime;

public record Item(long id, long ambienteId, String nome, String categoria,
                   String formaRegistro, boolean equipamento, int quantidade, String status,
                   String numeroPatrimonio, Long setorResponsavelId, String setorResponsavelNome,
                   String descricao, String localizacao, boolean temFoto, OffsetDateTime criadoEm, long versao, OffsetDateTime excluidoEm) { }
