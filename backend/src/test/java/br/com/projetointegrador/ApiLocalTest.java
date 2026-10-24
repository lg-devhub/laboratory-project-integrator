package br.com.projetointegrador;

import br.com.projetointegrador.database.Connect;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "stocka.testesLocais", matches = "true")
class ApiLocalTest {
    private static ConfigurableApplicationContext application;
    private static String base;
    private HttpClient client;

    @BeforeAll
    static void iniciarServidorDeTeste() {
        String url = Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url);
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka(\\?.*)?"),
                "Use somente o banco stocka local para estes testes.");
        application = SpringApplication.run(ApiApplication.class, "--server.port=0");
        base = "http://localhost:" + application.getEnvironment().getProperty("local.server.port");
    }

    @AfterAll
    static void pararServidorDeTeste() {
        if (application != null) application.close();
    }

    @BeforeEach
    void novoNavegador() {
        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    private HttpResponse<String> post(String rota, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + rota)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-STOCKA-Request", "true")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> me() throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/me"))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> login(String login, String senha) throws Exception {
        return post("/api/auth/login", "{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}");
    }

    private String cookie(HttpResponse<String> response) {
        return response.headers().allValues("set-cookie").stream()
                .filter(value -> value.startsWith("STOCKA_SESSION=")).findFirst().orElseThrow().split(";", 2)[0];
    }

    private HttpResponse<String> meComCookie(String cookie) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/api/auth/me"))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void loginCriaCookieProtegidoEPermiteConsultarConta() throws Exception {
        var resposta = login("admin@stocka.local", "Stocka123");
        assertEquals(200, resposta.statusCode());
        String cookie = resposta.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(resposta.body().contains("ADMINISTRADOR"));
        assertFalse(resposta.body().contains("senha"));
        assertFalse(resposta.body().contains("hash"));
        assertFalse(resposta.body().contains("Stocka123"));
        assertEquals("no-store", resposta.headers().firstValue("cache-control").orElseThrow());
        assertEquals(200, me().statusCode());
        assertTrue(me().body().contains("admin@stocka.local"));
    }

    @Test
    void credenciaisInvalidasEDesativacaoTemMesmaResposta() throws Exception {
        var errada = login("admin@stocka.local", "errada");
        var inexistente = login("inexistente@stocka.local", "Stocka123");
        var inativa = login("inativo@stocka.local", "Stocka123");
        assertEquals(401, errada.statusCode());
        assertEquals(401, inexistente.statusCode());
        assertEquals(401, inativa.statusCode());
        assertEquals(errada.body(), inexistente.body());
        assertEquals(errada.body(), inativa.body());
        assertEquals(401, me().statusCode());
    }

    @Test
    void consultaSemSessaoRetorna401() throws Exception {
        assertEquals(401, me().statusCode());
    }

    @Test
    void logoutInvalidaCookieAnterior() throws Exception {
        String cookie = cookie(login("operador@stocka.local", "Stocka123"));
        assertEquals(204, post("/api/auth/logout", "{}").statusCode());
        assertEquals(401, me().statusCode());
        assertEquals(401, meComCookie(cookie).statusCode());
        assertEquals(204, post("/api/auth/logout", "{}").statusCode());
    }

    @Test
    void novoLoginTrocaIdentificadorDaSessao() throws Exception {
        String anterior = cookie(login("admin@stocka.local", "Stocka123"));
        String novo = cookie(login("operador@stocka.local", "Stocka123"));
        assertNotEquals(anterior, novo);
        assertEquals(401, meComCookie(anterior).statusCode());
        assertTrue(me().body().contains("operador@stocka.local"));
    }

    @Test
    void tentativaInvalidaNaoMantemContaAnterior() throws Exception {
        assertEquals(200, login("admin@stocka.local", "Stocka123").statusCode());
        assertEquals(401, login("admin@stocka.local", "errada").statusCode());
        assertEquals(401, me().statusCode());
    }

    @Test
    void rejeitaJSONInvalidoECamposAusentes() throws Exception {
        assertEquals(400, post("/api/auth/login", "{").statusCode());
        assertEquals(400, post("/api/auth/login", "{}").statusCode());
        assertEquals(400, post("/api/auth/login", "null").statusCode());
    }

    @Test
    void rejeitaEscritaSemCabecalhoDeProtecao() throws Exception {
        var resposta = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/logout"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resposta.statusCode());
    }

    @Test
    void rejeitaOrigemDiferenteDaInterfacePermitida() throws Exception {
        var resposta = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Origin", "http://site-nao-permitido.example")
                .header("Content-Type", "application/json").header("X-STOCKA-Request", "true")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resposta.statusCode());
        assertTrue(resposta.headers().firstValue("access-control-allow-origin").isEmpty());
    }

    @Test
    void preflightPermiteVueComCookieECabecalho() throws Exception {
        var resposta = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-stocka-request")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resposta.statusCode());
        assertEquals("http://localhost:5173", resposta.headers().firstValue("access-control-allow-origin").orElseThrow());
        assertEquals("true", resposta.headers().firstValue("access-control-allow-credentials").orElseThrow());
    }

    @Test
    void contaSemAmbientesPodeEntrar() throws Exception {
        assertEquals(200, login("semacesso@stocka.local", "Stocka123").statusCode());
        assertEquals(200, me().statusCode());
    }

    @Test
    void reconsultaPerfilEDesativacaoDuranteSessao() throws Exception {
        // Conta exclusiva deste teste: nao altera as contas do grupo nem do seed.
        String login = "teste-api-" + UUID.randomUUID() + "@stocka.local";
        try (Connection connection = Connect.conexao()) {
            long id;
            try (var comando = connection.prepareStatement(
                    "INSERT INTO usuarios(nome,login,senha_hash,perfil) VALUES (?,?,?,'ADMINISTRADOR') RETURNING id")) {
                comando.setString(1, "Teste temporario API");
                comando.setString(2, login);
                comando.setString(3, BCrypt.hashpw("SenhaTeste123", BCrypt.gensalt(4)));
                try (var result = comando.executeQuery()) { result.next(); id = result.getLong(1); }
            }
            try {
                assertEquals(200, login(login, "SenhaTeste123").statusCode());
                try (var comando = connection.prepareStatement("UPDATE usuarios SET perfil='USUARIO' WHERE id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
                assertTrue(me().body().contains("USUARIO"));
                assertEquals(403, post("/api/ambientes", dadosAmbiente("Nao criar", "AZUL")).statusCode());
                assertEquals(200, getRota("/api/ambientes").statusCode());
                try (var comando = connection.prepareStatement("UPDATE usuarios SET ativo=false WHERE id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
                assertEquals(401, getRota("/api/ambientes").statusCode());
                assertEquals(401, me().statusCode());
            } finally {
                try (var comando = connection.prepareStatement("DELETE FROM usuarios WHERE id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
            }
        }
    }

    private HttpResponse<String> getRota(String rota) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + rota))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void ambientesSemSessaoRetornam401() throws Exception {
        assertEquals(401, getRota("/api/ambientes").statusCode());
    }

    @Test
    void administradorConsultaTodosOsAmbientesAtivos() throws Exception {
        assertEquals(200, login("admin@stocka.local", "Stocka123").statusCode());
        var resposta = getRota("/api/ambientes");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"nome\":\"Producao\""));
        assertTrue(resposta.body().contains("\"nome\":\"Sala de manutencao\""));
        assertFalse(resposta.body().contains("senha"));
    }

    @Test
    void usuarioComumConsultaSomenteAmbientesAtribuidos() throws Exception {
        assertEquals(200, login("operador@stocka.local", "Stocka123").statusCode());
        var resposta = getRota("/api/ambientes");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"nome\":\"Producao\""));
        assertFalse(resposta.body().contains("\"nome\":\"Sala de manutencao\""));
    }

    @Test
    void contaSemAtribuicoesRecebeListaVazia() throws Exception {
        assertEquals(200, login("semacesso@stocka.local", "Stocka123").statusCode());
        var resposta = getRota("/api/ambientes");
        assertEquals(200, resposta.statusCode());
        assertEquals("[]", resposta.body());
    }

    @Test
    void parametrosDoNavegadorNaoEscolhemIdentidadeOuPerfil() throws Exception {
        assertEquals(200, login("operador@stocka.local", "Stocka123").statusCode());
        assertEquals(getRota("/api/ambientes").body(),
                getRota("/api/ambientes?usuarioId=1&perfil=ADMINISTRADOR").body());
    }

    @Test
    void revogacaoDeAtribuicaoEAmbienteInativoRetiramAcesso() throws Exception {
        String nome = "Teste-hub-" + UUID.randomUUID();
        try (Connection connection = Connect.conexao()) {
            long id;
            try (var comando = connection.prepareStatement(
                    "INSERT INTO ambientes(nome,tipo) VALUES (?,'SALA') RETURNING id")) {
                comando.setString(1, nome);
                try (var result = comando.executeQuery()) { result.next(); id = result.getLong(1); }
            }
            try {
                try (var comando = connection.prepareStatement(
                        "INSERT INTO usuario_ambiente SELECT id,? FROM usuarios WHERE login='operador@stocka.local'")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
                assertEquals(200, login("operador@stocka.local", "Stocka123").statusCode());
                assertTrue(getRota("/api/ambientes").body().contains(nome));
                try (var comando = connection.prepareStatement("DELETE FROM usuario_ambiente WHERE ambiente_id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
                assertFalse(getRota("/api/ambientes").body().contains(nome));
                try (var comando = connection.prepareStatement("UPDATE ambientes SET ativo=false WHERE id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
                assertEquals(200, login("admin@stocka.local", "Stocka123").statusCode());
                assertFalse(getRota("/api/ambientes").body().contains(nome));
            } finally {
                try (var comando = connection.prepareStatement("DELETE FROM usuario_ambiente WHERE ambiente_id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
                try (var comando = connection.prepareStatement("DELETE FROM ambientes WHERE id=?")) {
                    comando.setLong(1, id); comando.executeUpdate();
                }
            }
        }
    }


    private HttpResponse<String> put(String rota, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + rota)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-STOCKA-Request", "true")
                .PUT(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String dadosAmbiente(String nome, String cor) {
        return "{\"nome\":\"" + nome + "\",\"tipo\":\"SALA\",\"descricao\":\"Teste de ambiente\",\"cor\":\"" + cor + "\"}";
    }

    private long criarAmbienteTemporario(String nome) throws Exception {
        var resposta = post("/api/ambientes", dadosAmbiente(nome, "AZUL"));
        assertEquals(201, resposta.statusCode(), resposta.body());
        String local = resposta.headers().firstValue("location").orElseThrow();
        return Long.parseLong(local.substring(local.lastIndexOf('/') + 1));
    }

    private void limparAmbiente(long id) throws Exception {
        try (var connection = Connect.conexao()) {
            try (var statement = connection.prepareStatement("DELETE FROM usuario_ambiente WHERE ambiente_id=?")) {
                statement.setLong(1, id); statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("DELETE FROM ambientes WHERE id=?")) {
                statement.setLong(1, id); statement.executeUpdate();
            }
        }
    }

    private long usuarioId(String login) throws Exception {
        try (var connection = Connect.conexao(); var statement = connection.prepareStatement("SELECT id FROM usuarios WHERE login=?")) {
            statement.setString(1, login);
            try (var result = statement.executeQuery()) { assertTrue(result.next()); return result.getLong(1); }
        }
    }

    @Test
    void gestaoDeAmbientesExigeAdministradorNoServidor() throws Exception {
        String dados = dadosAmbiente("Nao deve criar", "AZUL");
        assertEquals(401, post("/api/ambientes", dados).statusCode());
        assertEquals(401, put("/api/ambientes/1", dados).statusCode());
        assertEquals(401, getRota("/api/ambientes/1/acessos").statusCode());
        assertEquals(401, put("/api/ambientes/1/acessos", "{\"usuarios\":[]}").statusCode());
        login("operador@stocka.local", "Stocka123");
        assertEquals(403, post("/api/ambientes?perfil=ADMINISTRADOR", dados).statusCode());
        assertEquals(403, put("/api/ambientes/1", dados).statusCode());
        assertEquals(403, getRota("/api/ambientes/1/acessos").statusCode());
        assertEquals(403, put("/api/ambientes/1/acessos", "{\"usuarios\":[]}").statusCode());
    }

    @Test
    void criaEditaEPreservaCorSemPermitirNomesDuplicados() throws Exception {
        login("admin@stocka.local", "Stocka123");
        String nome = "Teste-gestao-" + UUID.randomUUID();
        long id = criarAmbienteTemporario(nome);
        try {
            assertTrue(getRota("/api/ambientes").body().contains(nome));
            assertEquals(409, post("/api/ambientes", dadosAmbiente(nome.toUpperCase(), "VERDE")).statusCode());
            assertEquals(409, put("/api/ambientes/" + id, dadosAmbiente("Producao", "VERDE")).statusCode());
            var atualizacao = put("/api/ambientes/" + id, dadosAmbiente("  " + nome + " editado  ", "VERDE"));
            assertEquals(200, atualizacao.statusCode());
            assertTrue(atualizacao.body().contains("\"cor\":\"VERDE\""));
            assertTrue(atualizacao.body().contains("\"nome\":\"" + nome + " editado\""));
            try (var connection = Connect.conexao(); var statement = connection.prepareStatement("SELECT cor FROM ambientes WHERE id=?")) {
                statement.setLong(1, id);
                try (var result = statement.executeQuery()) { assertTrue(result.next()); assertEquals("VERDE", result.getString(1)); }
            }
            assertEquals(404, put("/api/ambientes/9223372036854775807", dadosAmbiente(nome, "AZUL")).statusCode());
        } finally { limparAmbiente(id); }
    }

    @Test
    void validaDadosAntesDePersistir() throws Exception {
        login("admin@stocka.local", "Stocka123");
        assertEquals(400, post("/api/ambientes", dadosAmbiente("   ", "AZUL")).statusCode());
        assertEquals(400, post("/api/ambientes", dadosAmbiente("x".repeat(121), "AZUL")).statusCode());
        assertEquals(400, post("/api/ambientes", dadosAmbiente("Nome", "#ffffff")).statusCode());
        assertEquals(400, post("/api/ambientes", dadosAmbiente("Nome", "AZUL").replace("SALA", "FABRICA")).statusCode());
        assertEquals(400, post("/api/ambientes", "null").statusCode());
        assertEquals(400, post("/api/ambientes", "{}").statusCode());
        assertEquals(400, post("/api/ambientes", dadosAmbiente("Nome", "AZUL").replace("Teste de ambiente", "x".repeat(2001))).statusCode());
    }

    @Test
    void acessosSaoAtomicosEAtribuicaoRevogacaoValemDuranteSessao() throws Exception {
        login("admin@stocka.local", "Stocka123");
        String nome = "Teste-acessos-" + UUID.randomUUID();
        long id = criarAmbienteTemporario(nome);
        long operador = usuarioId("operador@stocka.local");
        long semAcesso = usuarioId("semacesso@stocka.local");
        long inativo = usuarioId("inativo@stocka.local");
        long admin = usuarioId("admin@stocka.local");
        var clienteAdmin = client;
        try {
            String rota = "/api/ambientes/" + id + "/acessos";
            var lista = getRota(rota);
            assertEquals(200, lista.statusCode());
            assertFalse(lista.body().contains("senha"));
            assertFalse(lista.body().contains("admin@stocka.local"));
            assertFalse(lista.body().contains("inativo@stocka.local"));
            assertEquals(204, put(rota, "{\"usuarios\":[" + operador + "," + semAcesso + "," + operador + "]}").statusCode());
            assertEquals(400, put(rota, "{\"usuarios\":[" + inativo + "]}").statusCode());
            assertEquals(400, put(rota, "{\"usuarios\":[" + admin + "]}").statusCode());
            assertEquals(400, put(rota, "{\"usuarios\":[9223372036854775807]}").statusCode());
            assertEquals(400, put(rota, "{\"usuarios\":[null]}").statusCode());
            assertEquals(400, put(rota, "{}").statusCode());
            client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            login("semacesso@stocka.local", "Stocka123");
            assertTrue(getRota("/api/ambientes").body().contains(nome));
            var clienteUsuario = client;
            client = clienteAdmin;
            assertEquals(204, put(rota, "{\"usuarios\":[]}").statusCode());
            client = clienteUsuario;
            assertFalse(getRota("/api/ambientes").body().contains(nome));
            assertEquals(200, me().statusCode());
            client = clienteAdmin;
            assertEquals(404, getRota("/api/ambientes/9223372036854775807/acessos").statusCode());
            assertEquals(404, put("/api/ambientes/9223372036854775807/acessos", "{\"usuarios\":[]}").statusCode());
        } finally { client = clienteAdmin; limparAmbiente(id); }
    }

    @Test
    void atribuicaoInativaExistentePodeSerPreservadaOuRevogada() throws Exception {
        login("admin@stocka.local", "Stocka123");
        long id = criarAmbienteTemporario("Teste-inativo-" + UUID.randomUUID());
        long inativo = usuarioId("inativo@stocka.local");
        String rota = "/api/ambientes/" + id + "/acessos";
        try {
            try (var connection = Connect.conexao(); var statement = connection.prepareStatement("INSERT INTO usuario_ambiente VALUES (?,?)")) {
                statement.setLong(1, inativo); statement.setLong(2, id); statement.executeUpdate();
            }
            assertTrue(getRota(rota).body().contains("inativo@stocka.local"));
            assertEquals(204, put(rota, "{\"usuarios\":[" + inativo + "]}").statusCode());
            assertEquals(204, put(rota, "{\"usuarios\":[]}").statusCode());
            assertEquals(400, put(rota, "{\"usuarios\":[" + inativo + "]}").statusCode());
        } finally { limparAmbiente(id); }
    }

    @Test
    void navegadorPodeRealizarPutComOrigemPermitida() throws Exception {
        var resposta = client.send(HttpRequest.newBuilder(URI.create(base + "/api/ambientes/1"))
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "content-type,x-stocka-request")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.headers().firstValue("access-control-allow-methods").orElseThrow().contains("PUT"));
    }

}
