package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.Manutencao;
import br.com.projetointegrador.model.ManutencaoDados;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ManutencaoDAO {
    private static final String SELECT_MANUTENCAO="""
        SELECT m.*,u.nome AS usuario_nome FROM public.manutencoes m
        JOIN public.usuarios u ON u.id=m.usuario_id
        JOIN public.itens i ON i.id=m.equipamento_id
        WHERE i.ambiente_id=? AND i.id=? AND i.excluido_em IS NULL
        """;
    private Manutencao ler(ResultSet r) throws SQLException {
        return new Manutencao(r.getLong("id"),r.getLong("equipamento_id"),r.getObject("ordem_servico_id",Long.class),
            r.getObject("data_realizacao",LocalDate.class),r.getString("descricao_servico"),r.getString("responsavel_tecnico"),
            r.getLong("usuario_id"),r.getString("usuario_nome"),r.getObject("registrado_em",OffsetDateTime.class));
    }
    public Optional<List<Manutencao>> consultar(long usuario,long ambiente,long item,Long id) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                if(!new ItemDAO().equipamentoAutorizado(c,usuario,ambiente,item)){c.rollback();return Optional.empty();}
                List<Manutencao> lista=new ArrayList<>();
                try(var s=c.prepareStatement(SELECT_MANUTENCAO+(id==null?"":" AND m.id=?")+" ORDER BY m.data_realizacao DESC,m.id DESC")) {
                    s.setLong(1,ambiente);s.setLong(2,item);if(id!=null)s.setLong(3,id);
                    try(var r=s.executeQuery()){while(r.next())lista.add(ler(r));}
                }
                c.commit();return Optional.of(lista);
            }catch(SQLException|RuntimeException erro){c.rollback();throw erro;}
        }
    }
    public Optional<Manutencao> criar(long usuario,long ambiente,long item,ManutencaoDados dados) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                if(!new ItemDAO().equipamentoAutorizado(c,usuario,ambiente,item)){c.rollback();return Optional.empty();}
                if(dados.ordemServicoId()!=null) {
                    try(var s=c.prepareStatement("SELECT id FROM public.ordens_servico WHERE id=? AND equipamento_id=? FOR SHARE")) {
                        s.setLong(1,dados.ordemServicoId());s.setLong(2,item);
                        try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("Selecione uma OS deste equipamento ou deixe sem vinculo.");}
                    }
                }
                long id;
                try(var s=c.prepareStatement("""
                    INSERT INTO public.manutencoes(equipamento_id,ordem_servico_id,data_realizacao,descricao_servico,responsavel_tecnico,usuario_id)
                    VALUES (?,?,?,?,?,?) RETURNING id
                    """)) {
                    s.setLong(1,item);s.setObject(2,dados.ordemServicoId());s.setObject(3,dados.dataRealizacao());
                    s.setString(4,dados.descricaoServico());s.setString(5,dados.responsavelTecnico());s.setLong(6,usuario);
                    try(var r=s.executeQuery()){r.next();id=r.getLong(1);}
                }
                Manutencao manutencao;
                try(var s=c.prepareStatement(SELECT_MANUTENCAO+" AND m.id=?")) {
                    s.setLong(1,ambiente);s.setLong(2,item);s.setLong(3,id);
                    try(var r=s.executeQuery()){r.next();manutencao=ler(r);}
                }
                c.commit();return Optional.of(manutencao);
            }catch(SQLException|RuntimeException erro){c.rollback();throw erro;}
        }
    }
}
