package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.Ambiente;
import br.com.projetointegrador.model.AmbienteDados;
import br.com.projetointegrador.model.AcessoAmbiente;
import br.com.projetointegrador.model.SetorOpcao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class AmbienteDAO {
    public List<SetorOpcao> listarSetoresResponsaveis() throws SQLException {
        String sql="SELECT id,nome FROM public.ambientes WHERE ativo AND tipo='SETOR' ORDER BY lower(nome),id";
        try (var connection=Connect.conexao(); var statement=connection.prepareStatement(sql); var result=statement.executeQuery()) {
            List<SetorOpcao> lista=new ArrayList<>();
            while (result.next()) lista.add(new SetorOpcao(result.getLong(1),result.getString(2)));
            return lista;
        }
    }

    public Optional<Ambiente> buscarAutorizado(long usuarioId, long ambienteId) throws SQLException {
        try (var connection = Connect.conexao()) {
            return buscarAutorizado(connection, usuarioId, ambienteId);
        }
    }

    // A escrita de itens reutiliza esta verificacao dentro da sua transacao.
    public Optional<Ambiente> buscarAutorizado(java.sql.Connection connection, long usuarioId, long ambienteId) throws SQLException {
        String sql = """
                SELECT a.* FROM public.usuarios u CROSS JOIN public.ambientes a
                WHERE u.id=? AND u.ativo AND a.id=? AND a.ativo
                  AND (u.perfil='ADMINISTRADOR' OR EXISTS (
                    SELECT 1 FROM public.usuario_ambiente ua WHERE ua.usuario_id=u.id AND ua.ambiente_id=a.id
                  )) FOR SHARE OF u,a
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1,usuarioId); statement.setLong(2,ambienteId);
            try (var result = statement.executeQuery()) { return result.next() ? Optional.of(ler(result)) : Optional.empty(); }
        }
    }

    private Ambiente ler(ResultSet result) throws SQLException {
        return new Ambiente(result.getLong("id"), result.getString("nome"), result.getString("tipo"),
                result.getString("descricao"), result.getString("cor"));
    }

    public List<Ambiente> listarAutorizados(long usuarioId) throws SQLException {
        String sql = """
                SELECT a.id, a.nome, a.tipo, a.descricao, a.cor
                FROM public.usuarios u CROSS JOIN public.ambientes a
                WHERE u.id = ? AND u.ativo AND a.ativo
                  AND (u.perfil = 'ADMINISTRADOR' OR EXISTS (
                    SELECT 1 FROM public.usuario_ambiente ua
                    WHERE ua.usuario_id = u.id AND ua.ambiente_id = a.id
                  ))
                ORDER BY lower(a.nome), a.id
                """;
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            try (var result = statement.executeQuery()) {
                List<Ambiente> ambientes = new ArrayList<>();
                while (result.next()) ambientes.add(ler(result));
                return ambientes;
            }
        }
    }

    public Ambiente criar(AmbienteDados dados) throws SQLException {
        String sql = "INSERT INTO public.ambientes(nome,tipo,descricao,cor) VALUES (?,?,?,?) RETURNING *";
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, dados.nome()); statement.setString(2, dados.tipo());
            statement.setString(3, dados.descricao()); statement.setString(4, dados.cor());
            try (var result = statement.executeQuery()) { result.next(); return ler(result); }
        }
    }

    public Optional<Ambiente> editar(long id, AmbienteDados dados) throws SQLException {
        String sql = "UPDATE public.ambientes SET nome=?,tipo=?,descricao=?,cor=? WHERE id=? AND ativo RETURNING *";
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, dados.nome()); statement.setString(2, dados.tipo());
            statement.setString(3, dados.descricao()); statement.setString(4, dados.cor()); statement.setLong(5, id);
            try (var result = statement.executeQuery()) { return result.next() ? Optional.of(ler(result)) : Optional.empty(); }
        }
    }

    public boolean ativo(long id) throws SQLException {
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement(
                "SELECT 1 FROM public.ambientes WHERE id=? AND ativo")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) { return result.next(); }
        }
    }

    public List<AcessoAmbiente> listarAcessos(long id) throws SQLException {
        String sql = """
                SELECT u.id,u.nome,u.login,u.ativo,(ua.usuario_id IS NOT NULL) AS atribuido
                FROM public.usuarios u LEFT JOIN public.usuario_ambiente ua
                  ON ua.usuario_id=u.id AND ua.ambiente_id=?
                WHERE u.perfil='USUARIO' AND (u.ativo OR ua.usuario_id IS NOT NULL)
                ORDER BY lower(u.nome),u.id
                """;
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                List<AcessoAmbiente> lista = new ArrayList<>();
                while (result.next()) lista.add(new AcessoAmbiente(result.getLong("id"), result.getString("nome"),
                        result.getString("login"), result.getBoolean("ativo"), result.getBoolean("atribuido")));
                return lista;
            }
        }
    }

    public boolean salvarAcessos(long id, List<Long> usuarios) throws SQLException {
        // Tudo ou nada: uma selecao invalida nao pode revogar os acessos anteriores.
        try (var connection = Connect.conexao()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("SELECT id FROM public.ambientes WHERE id=? AND ativo FOR UPDATE")) {
                    statement.setLong(1, id);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) { connection.rollback(); return false; }
                    }
                }
                var selecionados = new HashSet<>(usuarios);
                // Preserva atribuicoes de contas inativas se mantidas, mas nao cria novas.
                try (var statement = connection.prepareStatement("""
                        SELECT id FROM public.usuarios u WHERE id=? AND perfil='USUARIO'
                        AND (ativo OR EXISTS(SELECT 1 FROM public.usuario_ambiente WHERE usuario_id=u.id AND ambiente_id=?))
                        FOR UPDATE
                        """)) {
                    for (long usuario : selecionados.stream().sorted().toList()) {
                        statement.setLong(1, usuario); statement.setLong(2, id);
                        try (var result = statement.executeQuery()) {
                            if (!result.next()) throw new IllegalArgumentException("Selecione somente usuarios disponiveis. Atualize a lista e tente novamente.");
                        }
                    }
                }
                try (var statement = connection.prepareStatement("DELETE FROM public.usuario_ambiente WHERE ambiente_id=?")) {
                    statement.setLong(1, id); statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("INSERT INTO public.usuario_ambiente(usuario_id,ambiente_id) VALUES (?,?)")) {
                    for (long usuario : selecionados) {
                        statement.setLong(1, usuario); statement.setLong(2, id); statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException erro) {
                connection.rollback();
                throw erro;
            }
        }
    }
}
