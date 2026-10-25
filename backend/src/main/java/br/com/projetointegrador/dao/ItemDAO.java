package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.Item;
import br.com.projetointegrador.model.ItemDados;
import br.com.projetointegrador.model.ItemHistorico;
import br.com.projetointegrador.model.ItemEdicao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {
    // OS e manutencoes compartilham a regra de equipamento e o bloqueio transacional.
    public boolean equipamentoAutorizado(java.sql.Connection c,long usuario,long ambiente,long item) throws SQLException {
        if(new AmbienteDAO().buscarAutorizado(c,usuario,ambiente).isEmpty())return false;
        try(var s=c.prepareStatement("SELECT equipamento FROM public.itens WHERE id=? AND ambiente_id=? AND excluido_em IS NULL FOR SHARE")) {
            s.setLong(1,item);s.setLong(2,ambiente);
            try(var r=s.executeQuery()) {
                if(!r.next())return false;
                if(!r.getBoolean(1))throw new IllegalArgumentException("Este recurso pertence somente a equipamentos individuais.");
                return true;
            }
        }
    }

    private static final String SELECT_ITEM = """
            SELECT i.id,i.ambiente_id,i.nome,i.categoria,i.forma_registro,i.equipamento,i.quantidade,i.status,
                   i.numero_patrimonio,i.setor_responsavel_id,s.nome AS setor_nome,i.descricao,i.localizacao,
                   (i.foto IS NOT NULL) AS tem_foto,i.criado_em,i.versao
            FROM public.itens i LEFT JOIN public.ambientes s ON s.id=i.setor_responsavel_id
            """;

    private Item ler(ResultSet r) throws SQLException {
        return new Item(r.getLong("id"),r.getLong("ambiente_id"),r.getString("nome"),r.getString("categoria"),
                r.getString("forma_registro"),r.getBoolean("equipamento"),r.getInt("quantidade"),r.getString("status"),
                r.getString("numero_patrimonio"),r.getObject("setor_responsavel_id",Long.class),r.getString("setor_nome"),
                r.getString("descricao"),r.getString("localizacao"),r.getBoolean("tem_foto"),r.getObject("criado_em",OffsetDateTime.class),r.getLong("versao"));
    }

    public List<Item> listar(long ambienteId) throws SQLException {
        try (var connection=Connect.conexao(); var statement=connection.prepareStatement(
                SELECT_ITEM + " WHERE i.ambiente_id=? AND i.excluido_em IS NULL ORDER BY lower(i.nome),i.id")) {
            statement.setLong(1,ambienteId);
            try (var result=statement.executeQuery()) {
                List<Item> lista=new ArrayList<>();
                while (result.next()) lista.add(ler(result));
                return lista;
            }
        }
    }

    public Optional<Item> criar(long ambienteId, long usuarioId, ItemDados dados) throws SQLException {
        try (var connection=Connect.conexao()) {
            connection.setAutoCommit(false);
            try {
                if (new AmbienteDAO().buscarAutorizado(connection,usuarioId,ambienteId).isEmpty()) {
                    connection.rollback(); return Optional.empty();
                }
                if (dados.equipamento()) {
                    try (var s=connection.prepareStatement("SELECT id FROM public.ambientes WHERE id=? AND tipo='SETOR' AND ativo FOR SHARE")) {
                        s.setLong(1,dados.setorResponsavelId());
                        try (var r=s.executeQuery()) {
                            if (!r.next()) throw new IllegalArgumentException("Selecione um setor responsavel ativo.");
                        }
                    }
                }
                definirAutor(connection,usuarioId);
                long id;
                try (var s=connection.prepareStatement("""
                        INSERT INTO public.itens(ambiente_id,nome,categoria,forma_registro,equipamento,quantidade,status,
                        numero_patrimonio,setor_responsavel_id,descricao,localizacao,foto,foto_tipo)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id
                        """)) {
                    s.setLong(1,ambienteId); s.setString(2,dados.nome()); s.setString(3,dados.categoria());
                    s.setString(4,dados.formaRegistro()); s.setBoolean(5,dados.equipamento()); s.setInt(6,dados.quantidade());
                    s.setString(7,dados.status()); s.setString(8,dados.numeroPatrimonio()); s.setObject(9,dados.setorResponsavelId());
                    s.setString(10,dados.descricao()); s.setString(11,dados.localizacao());
                    s.setBytes(12,dados.foto()); s.setString(13,dados.fotoTipo());
                    try (var r=s.executeQuery()) { r.next(); id=r.getLong(1); }
                }
                Item item;
                try (var s=connection.prepareStatement(SELECT_ITEM + " WHERE i.id=?")) {
                    s.setLong(1,id);
                    try (var r=s.executeQuery()) { r.next(); item=ler(r); }
                }
                connection.commit(); // O trigger ja gravou a auditoria na mesma transacao.
                return Optional.of(item);
            } catch (SQLException | RuntimeException erro) {
                connection.rollback(); throw erro;
            }
        }
    }

    public List<ItemHistorico> historico(long ambienteId, long itemId) throws SQLException {
        String sql="""
                SELECT h.id,h.acao,h.usuario_id,u.nome,h.data_hora,h.status_anterior,h.status_novo FROM public.item_historico h
                JOIN public.usuarios u ON u.id=h.usuario_id JOIN public.itens i ON i.id=h.item_id
                WHERE i.ambiente_id=? AND i.id=? AND i.excluido_em IS NULL ORDER BY h.id DESC
                """;
        try (var connection=Connect.conexao(); var s=connection.prepareStatement(sql)) {
            s.setLong(1,ambienteId); s.setLong(2,itemId);
            try (var r=s.executeQuery()) {
                List<ItemHistorico> lista=new ArrayList<>();
                while (r.next()) lista.add(new ItemHistorico(r.getLong("id"),r.getString("acao"),r.getLong("usuario_id"),
                        r.getString("nome"),r.getObject("data_hora",OffsetDateTime.class),r.getString("status_anterior"),r.getString("status_novo")));
                return lista;
            }
        }
    }

    public record Foto(byte[] bytes, String tipo) { }

    private void definirAutor(java.sql.Connection connection,long usuarioId) throws SQLException {
        try (var s=connection.prepareStatement("SELECT set_config('stocka.usuario_id',?,true)")) {
            s.setString(1,Long.toString(usuarioId)); s.execute();
        }
    }

    public Optional<Item> buscar(long ambienteId,long itemId) throws SQLException {
        try (var connection=Connect.conexao();var s=connection.prepareStatement(
                SELECT_ITEM+" WHERE i.ambiente_id=? AND i.id=? AND i.excluido_em IS NULL")) {
            s.setLong(1,ambienteId);s.setLong(2,itemId);
            try(var r=s.executeQuery()){ return r.next()?Optional.of(ler(r)):Optional.empty(); }
        }
    }

    public static class ConflitoEdicao extends RuntimeException {
        public final Item atual;
        public ConflitoEdicao(Item atual) { super("O item foi alterado por outra operacao."); this.atual=atual; }
    }

    public Optional<Item> editar(long ambienteId,long itemId,long usuarioId,ItemEdicao edicao) throws SQLException {
        try (var connection=Connect.conexao()) {
            connection.setAutoCommit(false);
            try {
                if (new AmbienteDAO().buscarAutorizado(connection,usuarioId,ambienteId).isEmpty()) {
                    connection.rollback();return Optional.empty();
                }
                Item atual;
                try(var s=connection.prepareStatement(SELECT_ITEM+" WHERE i.ambiente_id=? AND i.id=? AND i.excluido_em IS NULL FOR UPDATE OF i")) {
                    s.setLong(1,ambienteId);s.setLong(2,itemId);
                    try(var r=s.executeQuery()){
                        if(!r.next()){ connection.rollback();return Optional.empty(); }
                        atual=ler(r);
                    }
                }
                // A segunda edicao espera o lock e compara a revisao ja atualizada.
                if(atual.versao()!=edicao.versao()) throw new ConflitoEdicao(atual);
                ItemDados dados=edicao.validar(atual);
                definirAutor(connection,usuarioId);
                try(var s=connection.prepareStatement("""
                        UPDATE public.itens SET nome=?,categoria=?,quantidade=?,status=?,descricao=?,localizacao=?,
                        foto=CASE WHEN ?='MANTER' THEN foto WHEN ?='REMOVER' THEN NULL ELSE ? END,
                        foto_tipo=CASE WHEN ?='MANTER' THEN foto_tipo WHEN ?='REMOVER' THEN NULL ELSE ? END
                        WHERE ambiente_id=? AND id=? AND versao=?
                        """)) {
                    s.setString(1,dados.nome());s.setString(2,dados.categoria());s.setInt(3,dados.quantidade());s.setString(4,dados.status());
                    s.setString(5,dados.descricao());s.setString(6,dados.localizacao());
                    s.setString(7,edicao.acaoFoto());s.setString(8,edicao.acaoFoto());s.setBytes(9,dados.foto());
                    s.setString(10,edicao.acaoFoto());s.setString(11,edicao.acaoFoto());s.setString(12,dados.fotoTipo());
                    s.setLong(13,ambienteId);s.setLong(14,itemId);s.setLong(15,edicao.versao());
                    s.executeUpdate();
                }
                Item salvo;
                try(var s=connection.prepareStatement(SELECT_ITEM+" WHERE i.id=?")) {
                    s.setLong(1,itemId);try(var r=s.executeQuery()){r.next();salvo=ler(r);}
                }
                connection.commit();
                return Optional.of(salvo);
            } catch(SQLException|RuntimeException erro){connection.rollback();throw erro;}
        }
    }

    public Optional<Foto> foto(long ambienteId,long itemId) throws SQLException {
        try (var connection=Connect.conexao(); var s=connection.prepareStatement(
                "SELECT foto,foto_tipo FROM public.itens WHERE ambiente_id=? AND id=? AND excluido_em IS NULL AND foto IS NOT NULL")) {
            s.setLong(1,ambienteId); s.setLong(2,itemId);
            try (var r=s.executeQuery()) { return r.next() ? Optional.of(new Foto(r.getBytes(1),r.getString(2))) : Optional.empty(); }
        }
    }
}
