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
class GestaoUsuarioLocalTest {
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
    private void admin() throws Exception {
        assertEquals(200,enviar("POST","/api/auth/login","{\"login\":\"admin@stocka.local\",\"senha\":\"Stocka123\"}",true).statusCode());
    }
    private String cadastro(String nome,String login,String perfil,String senha,Long versao) {
        return "{\"nome\":\""+nome+"\",\"login\":\""+login+"\",\"perfil\":\""+perfil+"\",\"senha\":"+(senha==null?"null":"\""+senha+"\"")+",\"versao\":"+versao+"}";
    }
    private long id(HttpResponse<String> r) {
        assertEquals(201,r.statusCode(),r.body());var m=java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(r.body());assertTrue(m.find());return Long.parseLong(m.group(1));
    }
    private long criar(String perfil) throws Exception {
        return id(enviar("POST","/api/usuarios",cadastro("Conta QA","qa-"+UUID.randomUUID()+"@stocka.local",perfil,"Stocka123",null),true));
    }
    private long idAdmin() throws Exception {return new UsuarioDAO().buscarPorLogin("admin@stocka.local").orElseThrow().getId();}
    private java.util.List<Long> retirarOutrosAdmins(long... preservar) throws Exception {
        java.util.List<Long> ids=new java.util.ArrayList<>();
        try(var c=Connect.conexao();var s=c.createStatement();var r=s.executeQuery("SELECT id FROM usuarios WHERE ativo AND perfil='ADMINISTRADOR'")){
            while(r.next()){long id=r.getLong(1);if(java.util.Arrays.stream(preservar).noneMatch(p->p==id))ids.add(id);}
        }
        try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE usuarios SET perfil='USUARIO' WHERE id=?")){for(long id:ids){s.setLong(1,id);s.executeUpdate();}}
        return ids;
    }
    private void restaurarAdmins(java.util.List<Long> ids) throws Exception {
        try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE usuarios SET perfil='ADMINISTRADOR' WHERE id=?")){for(long id:ids){s.setLong(1,id);s.executeUpdate();}}
    }
    @Test void somenteAdministradorListaECadastraSemExporHash() throws Exception {
        assertEquals(401,enviar("GET","/api/usuarios",null,false).statusCode());
        assertEquals(200,entrar("Stocka123"));
        assertEquals(403,enviar("GET","/api/usuarios",null,false).statusCode());
        assertEquals(403,enviar("POST","/api/usuarios",cadastro("Teste","novo@stocka.local","ADMINISTRADOR","Stocka123",null),true).statusCode());
        admin();var lista=enviar("GET","/api/usuarios",null,false);assertEquals(200,lista.statusCode());
        assertFalse(lista.body().contains("\"senha\":"));assertFalse(lista.body().contains("\"senha_hash\":"));assertFalse(lista.body().contains("\"senhaHash\":"));assertFalse(lista.body().contains("$2a$"));assertTrue(lista.body().contains("\"ativo\":false"));
    }
    @Test void cadastroValidaPoliticaEUnicidadeSemDiferenciarMaiusculas() throws Exception {
        admin();String novo="novo-"+UUID.randomUUID()+"@stocka.local";
        long id=id(enviar("POST","/api/usuarios",cadastro(" Novo usuario ",novo,"USUARIO","Novaá456",null),true));
        var conta=new UsuarioDAO().buscarPorId(id).orElseThrow();assertEquals("Novo usuario",conta.getNome());assertTrue(BCrypt.checkpw("Novaá456",conta.getSenhaHash()));
        assertEquals(409,enviar("POST","/api/usuarios",cadastro("Duplicado",novo.toUpperCase(),"USUARIO","Stocka123",null),true).statusCode());
        for(String senha:new String[]{"12345678","abcdefgh","Abc1","a1".repeat(37)})
            assertEquals(400,enviar("POST","/api/usuarios",cadastro("Invalido","invalid-"+UUID.randomUUID(),"USUARIO",senha,null),true).statusCode());
        assertEquals(400,enviar("POST","/api/usuarios",cadastro(" ","qualquer","USUARIO","Stocka123",null),true).statusCode());
        assertEquals(400,enviar("POST","/api/usuarios",cadastro("Nome"," ","USUARIO","Stocka123",null),true).statusCode());
        assertEquals(400,enviar("POST","/api/usuarios",cadastro("Nome","qualquer","OPERADOR","Stocka123",null),true).statusCode());
    }
    @Test void edicaoPreservaSenhaVaziaEPermiteNovaSenhaEPerfil() throws Exception {
        admin();String novo="edit-"+UUID.randomUUID();long id=id(enviar("POST","/api/usuarios",cadastro("Original",novo,"USUARIO","Stocka123",null),true));
        String hash=new UsuarioDAO().buscarPorId(id).orElseThrow().getSenhaHash();
        var alterada=enviar("PUT","/api/usuarios/"+id,cadastro("Revisado",novo,"ADMINISTRADOR","",1L),true);
        assertEquals(200,alterada.statusCode(),alterada.body());assertFalse(alterada.body().contains("senha"));assertTrue(alterada.body().contains("\"versao\":2"));
        assertEquals(hash,new UsuarioDAO().buscarPorId(id).orElseThrow().getSenhaHash());
        assertEquals(200,enviar("PUT","/api/usuarios/"+id,cadastro("Revisado",novo,"USUARIO","Nova4567",2L),true).statusCode());
        var service=new br.com.projetointegrador.service.AutenticacaoService();assertTrue(service.autenticar(novo,"Stocka123").isEmpty());assertTrue(service.autenticar(novo,"Nova4567").isPresent());
    }
    @Test void desativacaoPreservaIdentidadeEImpedeSessaoJaAberta() throws Exception {
        assertEquals(200,entrar("Stocka123"));HttpClient sessaoConta=client;client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();admin();
        assertEquals(200,enviar("POST","/api/usuarios/"+usuarioId+"/desativacao","{\"versao\":1}",true).statusCode());
        var usuario=new UsuarioDAO().buscarPorId(usuarioId).orElseThrow();assertFalse(usuario.isAtivo());assertEquals(login,usuario.getLogin());
        var me=sessaoConta.send(HttpRequest.newBuilder(URI.create(base+"/api/auth/me")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(401,me.statusCode());
        assertTrue(new br.com.projetointegrador.service.AutenticacaoService().autenticar(login,"Stocka123").isEmpty());
        assertEquals(409,enviar("POST","/api/usuarios",cadastro("Duplicado",login,"USUARIO","Stocka123",null),true).statusCode());
    }
    @Test void ultimoAdministradorNaoPodeSerDesativadoOuRebaixado() throws Exception {
        admin();long adminId=idAdmin();var outros=retirarOutrosAdmins(adminId);
        try {
            long versao;
            try(var c=Connect.conexao();var s=c.prepareStatement("SELECT versao FROM usuarios WHERE id=?")){s.setLong(1,adminId);try(var r=s.executeQuery()){r.next();versao=r.getLong(1);}}
            var desativar=enviar("POST","/api/usuarios/"+adminId+"/desativacao","{\"versao\":"+versao+"}",true);assertEquals(409,desativar.statusCode());assertTrue(desativar.body().contains("ULTIMO_ADMINISTRADOR"));
            assertEquals(409,enviar("PUT","/api/usuarios/"+adminId,cadastro("Administrador de teste","admin@stocka.local","USUARIO",null,versao),true).statusCode());
            assertTrue(new UsuarioDAO().buscarPorId(adminId).orElseThrow().isAtivo());
            try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE usuarios SET ativo=false WHERE id=?")){s.setLong(1,adminId);assertThrows(java.sql.SQLException.class,s::executeUpdate);}
        }finally{restaurarAdmins(outros);}
    }
    @Test void versaoDesatualizadaNaoSobrescreveContaNemTrocaDeSenha() throws Exception {
        admin();
        String nome=new UsuarioDAO().buscarPorId(usuarioId).orElseThrow().getNome();
        assertEquals(200,enviar("PUT","/api/usuarios/"+usuarioId,cadastro("Conta revisada",login,"USUARIO",null,1L),true).statusCode());
        assertEquals(409,enviar("PUT","/api/usuarios/"+usuarioId,cadastro("Antigo",login,"USUARIO",null,1L),true).statusCode());
        assertEquals(409,enviar("POST","/api/usuarios/"+usuarioId+"/desativacao","{\"versao\":1}",true).statusCode());
        assertTrue(new br.com.projetointegrador.service.AutenticacaoService().alterarSenha(usuarioId,"Stocka123","Outra456","Outra456"));
        assertEquals(409,enviar("PUT","/api/usuarios/"+usuarioId,cadastro("Antigo",login,"USUARIO",null,2L),true).statusCode());
        assertEquals(400,enviar("PUT","/api/usuarios/"+usuarioId,"{}",true).statusCode());
    }
    @Test void contaDesativadaNaoEhReativadaPorCampoExtra() throws Exception {
        admin();assertEquals(200,enviar("POST","/api/usuarios/"+usuarioId+"/desativacao","{\"versao\":1}",true).statusCode());
        String json=cadastro("Corrigido",login,"USUARIO",null,2L).replace("\"nome\"","\"ativo\":true,\"nome\"");
        assertEquals(200,enviar("PUT","/api/usuarios/"+usuarioId,json,true).statusCode());assertFalse(new UsuarioDAO().buscarPorId(usuarioId).orElseThrow().isAtivo());
    }
    @Test void doisAdministradoresNaoConseguemDesativarUmAoOutroAteZerarAdministracao() throws Exception {
        admin();long a=criar("ADMINISTRADOR"),b=criar("ADMINISTRADOR");var outros=retirarOutrosAdmins(a,b);
        try {
            String loginA=new UsuarioDAO().buscarPorId(a).orElseThrow().getLogin(),loginB=new UsuarioDAO().buscarPorId(b).orElseThrow().getLogin();
            HttpClient ca=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build(),cb=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            for(var par:java.util.Map.of(ca,loginA,cb,loginB).entrySet()){
                var req=HttpRequest.newBuilder(URI.create(base+"/api/auth/login")).header("Content-Type","application/json").header("X-STOCKA-Request","true").POST(HttpRequest.BodyPublishers.ofString("{\"login\":\""+par.getValue()+"\",\"senha\":\"Stocka123\"}")).build();
                assertEquals(200,par.getKey().send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
            }
            var ra=ca.sendAsync(HttpRequest.newBuilder(URI.create(base+"/api/usuarios/"+b+"/desativacao")).header("Content-Type","application/json").header("X-STOCKA-Request","true").POST(HttpRequest.BodyPublishers.ofString("{\"versao\":1}")).build(),HttpResponse.BodyHandlers.ofString());
            var rb=cb.sendAsync(HttpRequest.newBuilder(URI.create(base+"/api/usuarios/"+a+"/desativacao")).header("Content-Type","application/json").header("X-STOCKA-Request","true").POST(HttpRequest.BodyPublishers.ofString("{\"versao\":1}")).build(),HttpResponse.BodyHandlers.ofString());
            int sa=ra.get().statusCode(),sb=rb.get().statusCode();assertTrue((sa==200 && (sb==401||sb==403))||(sb==200 && (sa==401||sa==403)),sa+"/"+sb);
            try(var c=Connect.conexao();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM usuarios WHERE ativo AND perfil='ADMINISTRADOR'")){r.next();assertEquals(1,r.getLong(1));}
        }finally{restaurarAdmins(outros);}
    }
}
