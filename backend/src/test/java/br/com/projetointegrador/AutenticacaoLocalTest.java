package br.com.projetointegrador;

import br.com.projetointegrador.model.UsuarioAutenticado;
import br.com.projetointegrador.service.AutenticacaoService;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

// Usa somente os dados ficticios do seed local. Nenhum teste grava no banco.
@EnabledIfSystemProperty(named = "stocka.testesLocais", matches = "true")
class AutenticacaoLocalTest {
    private final AutenticacaoService service = new AutenticacaoService();

    @BeforeAll
    static void exigirBancoLocal() {
        String url = Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url, "Configure backend/.env antes de executar os testes.");
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka(\\?.*)?"),
                "Estes testes exigem o banco stocka local; nao execute contra Supabase.");
    }

    @Test
    void aceitaAdministradorComSenhaCorreta() throws SQLException {
        UsuarioAutenticado usuario = service.autenticar("admin@stocka.local", "Stocka123").orElseThrow();
        assertTrue(usuario.id() > 0);
        assertEquals("admin@stocka.local", usuario.login());
        assertEquals("ADMINISTRADOR", usuario.perfil());
    }

    @Test
    void aceitaOperadorMantendoPerfilComum() throws SQLException {
        UsuarioAutenticado usuario = service.autenticar("operador@stocka.local", "Stocka123").orElseThrow();
        assertEquals("USUARIO", usuario.perfil());
        assertEquals("operador@stocka.local", usuario.login());
    }

    @Test
    void aceitaContaAtivaMesmoSemAmbientesAtribuidos() throws SQLException {
        assertTrue(service.autenticar("semacesso@stocka.local", "Stocka123").isPresent());
    }

    @Test
    void rejeitaSenhaIncorreta() throws SQLException {
        assertTrue(service.autenticar("admin@stocka.local", "senha-incorreta").isEmpty());
    }

    @Test
    void rejeitaUsuarioInexistente() throws SQLException {
        assertTrue(service.autenticar("naoexiste@stocka.local", "Stocka123").isEmpty());
    }

    @Test
    void rejeitaContaDesativadaMesmoComSenhaCorreta() throws SQLException {
        assertTrue(service.autenticar("inativo@stocka.local", "Stocka123").isEmpty());
    }

    @Test
    void loginSegueUnicidadeSemDiferenciarMaiusculas() throws SQLException {
        assertTrue(service.autenticar("  ADMIN@STOCKA.LOCAL  ", "Stocka123").isPresent());
    }

    @Test
    void naoModificaASenhaRecebida() throws SQLException {
        assertTrue(service.autenticar("admin@stocka.local", "stocka123").isEmpty());
        assertTrue(service.autenticar("admin@stocka.local", "Stocka123 ").isEmpty());
        assertTrue(service.autenticar("admin@stocka.local", " Stocka123").isEmpty());
    }

    @Test
    void textoSQLNaoContornaCredenciais() throws SQLException {
        assertTrue(service.autenticar("' OR TRUE --", "Stocka123").isEmpty());
        assertTrue(service.autenticar("admin@stocka.local", "' OR TRUE --").isEmpty());
    }

    @Test
    void rejeitaCamposAusentesEVazios() throws SQLException {
        assertTrue(service.autenticar(null, "Stocka123").isEmpty());
        assertTrue(service.autenticar("  ", "Stocka123").isEmpty());
        assertTrue(service.autenticar("admin@stocka.local", null).isEmpty());
        assertTrue(service.autenticar("admin@stocka.local", "").isEmpty());
    }

    @Test
    void rejeitaSenhaQueExcedeLimiteTecnicoDoBCrypt() throws SQLException {
        assertTrue(service.autenticar("admin@stocka.local", "a".repeat(73)).isEmpty());
        assertTrue(service.autenticar("admin@stocka.local", "á".repeat(37)).isEmpty());
    }
}
