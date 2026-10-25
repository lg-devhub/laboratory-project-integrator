package br.com.projetointegrador.model;

import java.util.List;

public record OrdemServicoDados(String problema, String prioridade) {
    public OrdemServicoDados validar() {
        if (problema==null || problema.isBlank() || problema.strip().length()>2000 || problema.indexOf('\0')>=0)
            throw new IllegalArgumentException("Descreva o problema em ate 2000 caracteres.");
        if (prioridade==null || !List.of("BAIXA","MEDIA","ALTA").contains(prioridade))
            throw new IllegalArgumentException("Selecione a prioridade: Baixa, Media ou Alta.");
        return new OrdemServicoDados(problema.strip(),prioridade);
    }
}
