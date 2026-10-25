package br.com.projetointegrador.model;

public record OrdemServicoEdicao(String problema, String prioridade, String status,
                                 String descricaoAtendimento, Long versao) {
    public OrdemServicoEdicao validar(OrdemServico atual) {
        var campos=new OrdemServicoDados(problema,prioridade).validar();
        boolean mesmo=atual.status().equals(status);
        boolean iniciar=atual.status().equals("ABERTA") && "EM_ANDAMENTO".equals(status);
        boolean concluir=atual.status().equals("EM_ANDAMENTO") && "CONCLUIDA".equals(status);
        if(!mesmo && !iniciar && !concluir)
            throw new IllegalArgumentException("Siga o fluxo Aberta, Em Andamento e Concluida, sem pular etapas ou retornar.");
        String atendimento=descricaoAtendimento==null?null:descricaoAtendimento.strip();
        if("CONCLUIDA".equals(status)) {
            if(atendimento==null || atendimento.isBlank() || atendimento.length()>2000 || atendimento.indexOf('\0')>=0)
                throw new IllegalArgumentException("Para concluir, descreva o atendimento em ate 2000 caracteres.");
        } else if(atendimento!=null && !atendimento.isEmpty())
            throw new IllegalArgumentException("Informe a descricao do atendimento ao concluir a OS.");
        else atendimento=null;
        return new OrdemServicoEdicao(campos.problema(),campos.prioridade(),status,atendimento,versao);
    }
}
