package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.OrdemServico;
import br.com.projetointegrador.model.OrdemServicoDados;
import br.com.projetointegrador.model.OrdemServicoEdicao;
import br.com.projetointegrador.model.ResumoOrdensServico;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrdemServicoDAO {
    public Optional<List<ResumoOrdensServico>> resumirAmbiente(long usuario,long ambiente) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                if(new AmbienteDAO().buscarAutorizado(c,usuario,ambiente).isEmpty()){c.rollback();return Optional.empty();}
                List<ResumoOrdensServico> lista=new ArrayList<>();
                try(var s=c.prepareStatement("""
                    SELECT i.id,count(*) FILTER(WHERE o.status='ABERTA') AS abertas,
                        count(*) FILTER(WHERE o.status='EM_ANDAMENTO') AS andamento,
                        count(*) FILTER(WHERE o.status='CONCLUIDA') AS concluidas
                    FROM public.itens i JOIN public.ordens_servico o ON o.equipamento_id=i.id
                    WHERE i.ambiente_id=? AND i.equipamento AND i.excluido_em IS NULL
                    GROUP BY i.id ORDER BY i.id
                    """)) {
                    s.setLong(1,ambiente);
                    try(var r=s.executeQuery()){while(r.next())lista.add(new ResumoOrdensServico(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4)));}
                }
                c.commit();return Optional.of(lista);
            }catch(SQLException|RuntimeException erro){c.rollback();throw erro;}
        }
    }

    private static final String SELECT_OS="""
        SELECT o.*,u.nome AS solicitante_nome FROM public.ordens_servico o
        JOIN public.usuarios u ON u.id=o.solicitante_id
        JOIN public.itens i ON i.id=o.equipamento_id
        WHERE i.ambiente_id=? AND i.id=?
        """;

    private OrdemServico ler(ResultSet r) throws SQLException {
        return new OrdemServico(r.getLong("id"),r.getLong("equipamento_id"),r.getLong("solicitante_id"),
                r.getString("solicitante_nome"),r.getString("problema"),r.getString("prioridade"),
                r.getString("status"),r.getObject("aberta_em",OffsetDateTime.class),
                r.getString("descricao_atendimento"),r.getObject("fechada_em",OffsetDateTime.class),r.getLong("versao"));
    }

    // O acesso e os vinculos sao verificados na mesma transacao da consulta/gravação.
    public Optional<List<OrdemServico>> consultar(long usuario,long ambiente,long item,Long ordem) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                if(!new ItemDAO().equipamentoAutorizado(c,usuario,ambiente,item,true)){c.rollback();return Optional.empty();}
                List<OrdemServico> lista=new ArrayList<>();
                try(var s=c.prepareStatement(SELECT_OS+(ordem==null?"":" AND o.id=?")+" ORDER BY o.id DESC")) {
                    s.setLong(1,ambiente);s.setLong(2,item);if(ordem!=null)s.setLong(3,ordem);
                    try(var r=s.executeQuery()){while(r.next())lista.add(ler(r));}
                }
                c.commit();return Optional.of(lista);
            } catch(SQLException|RuntimeException erro){c.rollback();throw erro;}
        }
    }

    public Optional<OrdemServico> criar(long usuario,long ambiente,long item,OrdemServicoDados dados) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                if(!new ItemDAO().equipamentoAutorizado(c,usuario,ambiente,item)){c.rollback();return Optional.empty();}
                long id;
                // Solicitante vem da sessao; status/data usam os valores padrao do banco.
                try(var s=c.prepareStatement("INSERT INTO public.ordens_servico(equipamento_id,solicitante_id,problema,prioridade) VALUES (?,?,?,?) RETURNING id")) {
                    s.setLong(1,item);s.setLong(2,usuario);s.setString(3,dados.problema());s.setString(4,dados.prioridade());
                    try(var r=s.executeQuery()){r.next();id=r.getLong(1);}
                }
                OrdemServico ordem;
                try(var s=c.prepareStatement(SELECT_OS+" AND o.id=?")) {
                    s.setLong(1,ambiente);s.setLong(2,item);s.setLong(3,id);
                    try(var r=s.executeQuery()){r.next();ordem=ler(r);}
                }
                c.commit();return Optional.of(ordem);
            } catch(SQLException|RuntimeException erro){c.rollback();throw erro;}
        }
    }

    public static class Conflito extends RuntimeException {
        public final OrdemServico atual;
        public Conflito(OrdemServico atual) { super("OS alterada ou concluida.");this.atual=atual; }
    }

    public Optional<OrdemServico> atualizar(long usuario,long ambiente,long item,long ordemId,OrdemServicoEdicao edicao) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                if(!new ItemDAO().equipamentoAutorizado(c,usuario,ambiente,item)){c.rollback();return Optional.empty();}
                OrdemServico atual;
                try(var s=c.prepareStatement(SELECT_OS+" AND o.id=? FOR UPDATE OF o")) {
                    s.setLong(1,ambiente);s.setLong(2,item);s.setLong(3,ordemId);
                    try(var r=s.executeQuery()){if(!r.next()){c.rollback();return Optional.empty();}atual=ler(r);}
                }
                if(atual.versao()!=edicao.versao() || atual.status().equals("CONCLUIDA")) throw new Conflito(atual);
                var dados=edicao.validar(atual);
                try(var s=c.prepareStatement("UPDATE public.ordens_servico SET problema=?,prioridade=?,status=?,descricao_atendimento=? WHERE id=?")) {
                    s.setString(1,dados.problema());s.setString(2,dados.prioridade());s.setString(3,dados.status());
                    s.setString(4,dados.descricaoAtendimento());s.setLong(5,ordemId);s.executeUpdate();
                }
                OrdemServico salva;
                try(var s=c.prepareStatement(SELECT_OS+" AND o.id=?")) {
                    s.setLong(1,ambiente);s.setLong(2,item);s.setLong(3,ordemId);
                    try(var r=s.executeQuery()){r.next();salva=ler(r);}
                }
                c.commit();return Optional.of(salva);
            }catch(SQLException|RuntimeException erro){c.rollback();throw erro;}
        }
    }
}
