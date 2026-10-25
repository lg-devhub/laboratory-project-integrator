package br.com.projetointegrador;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.dao.UsuarioDAO;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

// Compartilha somente o banco descartavel do script; nao troca senhas do banco de trabalho.
@EnabledIfSystemProperty(named="stocka.testesInventario", matches="true")
class AlteracaoSenhaLocalTest {
    private static ConfigurableApplicationContext application;
    private static String base;
    private HttpClient client;
    private String login;
    private long usuarioId;

    @BeforeAll static void iniciar() {
        String url=Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url);
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka_testes_inventario_[a-f0-9]{12}(\\?.*)?"));
        application=SpringApplication.run(ApiApplication.class,"--server.port=0");
        base="http://localhost:"+application.getEnvironment().getProperty("local.server.port");
    }
    @AfterAll static void parar() { if(application!=null) application.close(); }
    @BeforeEach void preparar() throws Exception {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        login="senha-"+UUID.randomUUID()+"@stocka.local";
        try(var c=Connect.conexao();var s=c.prepareStatement(
                "INSERT INTO usuarios(nome,login,senha_hash,perfil,ativo) VALUES ('Teste de senha',?,?,'USUARIO',true) RETURNING id")) {
            s.setString(1,login);s.setString(2,BCrypt.hashpw("Stocka123",BCrypt.gensalt()));
            try(var r=s.executeQuery()){r.next();usuarioId=r.getLong(1);}
        }
    }
    private HttpResponse<String> enviar(String metodo,String rota,String json,boolean cabecalho) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+rota)).timeout(Duration.ofSeconds(10));
        if(json!=null) b.header("Content-Type","application/json");
        if(cabecalho) b.header("X-STOCKA-Request","true");
        return client.send(b.method(metodo,json==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private int entrar(String senha) throws Exception {
        return enviar("POST","/api/auth/login","{\"login\":\""+login+"\",\"senha\":\""+senha+"\"}",true).statusCode();
    }
    private String dados(String atual,String nova,String confirmacao) {
        return "{\"senhaAtual\":\""+atual+"\",\"novaSenha\":\""+nova+"\",\"confirmacao\":\""+confirmacao+"\",\"usuarioId\":1,\"perfil\":\"ADMINISTRADOR\"}";
    }
    @Test void exigeSessaoECabecalhoDeEscrita() throws Exception {
        String json=dados("Stocka123","Outra456","Outra456");
        assertEquals(401,enviar("PUT","/api/auth/senha",json,true).statusCode());
        assertEquals(200,entrar("Stocka123"));
        assertEquals(403,enviar("PUT","/api/auth/senha",json,false).statusCode());
        assertEquals(200,entrar("Stocka123"));
    }
    @Test void rejeitaDadosInvalidosSemMudarHash() throws Exception {
        assertEquals(200,entrar("Stocka123"));
        String hash=new UsuarioDAO().buscarPorId(usuarioId).orElseThrow().getSenhaHash();
        for(String json:new String[]{"null","{}",dados("errada","Outra456","Outra456"),
                dados("Stocka123","Curta1","Curta1"),dados("Stocka123","Somenteletras","Somenteletras"),
                dados("Stocka123","12345678","12345678"),dados("Stocka123","Outra456","Diferente9"),
                dados("Stocka123","a1".repeat(37),"a1".repeat(37)),
                dados("Stocka123","á".repeat(36)+"1","á".repeat(36)+"1")})
            assertEquals(400,enviar("PUT","/api/auth/senha",json,true).statusCode());
        assertEquals(hash,new UsuarioDAO().buscarPorId(usuarioId).orElseThrow().getSenhaHash());
    }
    @Test void trocaSomentePropriaSenhaComHashENovoLoginParaAmbosPerfis() throws Exception {
        String hashAdmin=new UsuarioDAO().buscarPorLogin("admin@stocka.local").orElseThrow().getSenhaHash();
        for(String perfil:new String[]{"USUARIO","ADMINISTRADOR"}) {
            try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE usuarios SET perfil=? WHERE id=?")) {
                s.setString(1,perfil);s.setLong(2,usuarioId);s.executeUpdate();
            }
            assertEquals(200,entrar("Stocka123"));
            assertEquals(204,enviar("PUT","/api/auth/senha",dados("Stocka123","Novaá456","Novaá456"),true).statusCode());
            assertEquals(200,enviar("GET","/api/auth/me",null,false).statusCode());
            var usuario=new UsuarioDAO().buscarPorId(usuarioId).orElseThrow();
            assertEquals(perfil,usuario.getPerfil());
            assertNotEquals("Novaá456",usuario.getSenhaHash());
            assertTrue(BCrypt.checkpw("Novaá456",usuario.getSenhaHash()));
            assertEquals(401,entrar("Stocka123"));
            assertEquals(200,entrar("Novaá456"));
            assertEquals(204,enviar("PUT","/api/auth/senha",dados("Novaá456","Stocka123","Stocka123"),true).statusCode());
        }
        assertEquals(hashAdmin,new UsuarioDAO().buscarPorLogin("admin@stocka.local").orElseThrow().getSenhaHash());
    }
    @Test void hashDesatualizadoNaoSobrescreveTrocaJaConcluida() throws Exception {
        var dao=new UsuarioDAO();String hash=dao.buscarPorId(usuarioId).orElseThrow().getSenhaHash();
        String novo=BCrypt.hashpw("Outra456",BCrypt.gensalt());
        assertTrue(dao.alterarSenha(usuarioId,hash,novo));
        assertFalse(dao.alterarSenha(usuarioId,hash,BCrypt.hashpw("Perdida789",BCrypt.gensalt())));
        assertEquals(novo,dao.buscarPorId(usuarioId).orElseThrow().getSenhaHash());
    }
}
