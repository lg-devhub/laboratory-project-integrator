package br.com.projetointegrador.model;

import java.time.LocalDate;

public record ManutencaoDados(LocalDate dataRealizacao,String descricaoServico,String responsavelTecnico,Long ordemServicoId) {
    public ManutencaoDados validar() {
        if(dataRealizacao==null || dataRealizacao.getYear()<1 || dataRealizacao.getYear()>9999)
            throw new IllegalArgumentException("Informe uma data de realizacao valida.");
        if(descricaoServico==null || descricaoServico.isBlank() || descricaoServico.strip().length()>2000 || descricaoServico.indexOf('\0')>=0)
            throw new IllegalArgumentException("Descreva o servico realizado em ate 2000 caracteres.");
        if(responsavelTecnico==null || responsavelTecnico.isBlank() || responsavelTecnico.strip().length()>120 || responsavelTecnico.indexOf('\0')>=0)
            throw new IllegalArgumentException("Informe o responsavel tecnico em ate 120 caracteres.");
        if(ordemServicoId!=null && ordemServicoId<=0)throw new IllegalArgumentException("Selecione uma OS valida ou deixe sem vinculo.");
        return new ManutencaoDados(dataRealizacao,descricaoServico.strip(),responsavelTecnico.strip(),ordemServicoId);
    }
}
