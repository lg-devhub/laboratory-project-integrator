package br.com.projetointegrador;

import br.com.projetointegrador.database.Connect;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.net.*;
import java.net.http.*;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="stocka.testesInventario",matches="true")
class RetiradaAmbienteLocalTest {
    private static ConfigurableApplicationContext application;
    private static String base;
    private HttpClient client;
    private long sala, setor;

    @BeforeAll static void iniciar() {
        String url=Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url);
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka_testes_inventario_[a-f0-9]{12}(\\?.*)?"));
        application=SpringApplication.run(ApiApplication.class,"--server.port=0");
        base="http://localhost:"+application.getEnvironment().getProperty("local.server.port");
    }
    @AfterAll static void parar(){if(application!=null)application.close();}
    @BeforeEach void preparar() throws Exception {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        entrar("admin");
        sala=id(enviar("POST","/api/ambientes",dados("Sala QA "+UUID.randomUUID(),"SALA"),true));
        setor=id(enviar("POST","/api/ambientes",dados("Setor QA "+UUID.randomUUID(),"SETOR"),true));
    }
    private HttpResponse<String> enviar(String metodo,String rota,String json,boolean cabecalho) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+rota)).timeout(Duration.ofSeconds(10));
        if(json!=null)b.header("Content-Type","application/json");
        if(cabecalho)b.header("X-STOCKA-Request","true");
        return client.send(b.method(metodo,json==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private void entrar(String nome) throws Exception {
        assertEquals(200,enviar("POST","/api/auth/login","{\"login\":\""+nome+"@stocka.local\",\"senha\":\"Stocka123\"}",true).statusCode());
    }
    private String dados(String nome,String tipo){return "{\"nome\":\""+nome+"\",\"tipo\":\""+tipo+"\",\"cor\":\"AZUL\"}";}
    private long id(HttpResponse<String> r){assertEquals(201,r.statusCode(),r.body());var m=Pattern.compile("\"id\":(\\d+)").matcher(r.body());assertTrue(m.find());return Long.parseLong(m.group(1));}
    private String rota(long id){return "/api/ambientes/"+id;}
    private HttpResponse<String> retirar(long id,int versao)throws Exception{return enviar("DELETE",rota(id)+"?versao="+versao,null,true);}
    private long item(String status,boolean equipamento) throws Exception {
        return id(enviar("POST",rota(sala)+"/itens","{\"nome\":\"Item QA\",\"categoria\":\"Teste\",\"formaRegistro\":\"INDIVIDUAL\",\"quantidade\":1,\"status\":\""+status+"\",\"equipamento\":"+equipamento+(equipamento?",\"numeroPatrimonio\":\"RET-"+UUID.randomUUID()+"\",\"setorResponsavelId\":"+setor:"")+"}",true));
    }
    @Test void exigeAdministradorCabecalhoEVersaoValida() throws Exception {
        enviar("POST","/api/auth/logout","{}",true);
        assertEquals(401,retirar(sala,1).statusCode());
        entrar("operador");assertEquals(403,retirar(sala,1).statusCode());
        entrar("admin");assertEquals(403,enviar("DELETE",rota(sala)+"?versao=1",null,false).statusCode());
        assertEquals(400,retirar(sala,0).statusCode());
        assertEquals(400,enviar("DELETE",rota(sala),null,true).statusCode());
        assertEquals(404,retirar(Long.MAX_VALUE,1).statusCode());
    }
    @Test void retiraVazioPreservaCadastroAtribuicaoELoginReservado() throws Exception {
        String nome;
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT nome FROM ambientes WHERE id=?")){
            s.setLong(1,sala);try(var r=s.executeQuery()){r.next();nome=r.getString(1);}
        }
        try(var c=Connect.conexao();var s=c.prepareStatement("INSERT INTO usuario_ambiente(usuario_id,ambiente_id) SELECT id,? FROM usuarios WHERE login='operador@stocka.local'")){s.setLong(1,sala);s.executeUpdate();}
        assertEquals(204,retirar(sala,1).statusCode());assertEquals(404,retirar(sala,1).statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT a.ativo,a.versao,count(ua.usuario_id) AS atribuicoes FROM ambientes a LEFT JOIN usuario_ambiente ua ON ua.ambiente_id=a.id WHERE a.id=? GROUP BY a.id")){
            s.setLong(1,sala);try(var r=s.executeQuery()){assertTrue(r.next());assertFalse(r.getBoolean(1));assertEquals(2,r.getLong(2));assertEquals(1,r.getInt(3));}
        }
        assertEquals(409,enviar("POST","/api/ambientes",dados(nome,"SALA"),true).statusCode());
        assertEquals(404,enviar("GET",rota(sala)+"/itens",null,false).statusCode());
        assertEquals(404,enviar("GET",rota(sala)+"/acessos",null,false).statusCode());
        entrar("operador");assertEquals(404,enviar("GET",rota(sala)+"/itens",null,false).statusCode());
    }
    @Test void bloqueiaItensDeQualquerStatusEExcluidosLogicamente() throws Exception {
        for(String status:new String[]{"ATIVO","EM_MANUTENCAO","INATIVO"})item(status,false);
        var r=retirar(sala,1);assertEquals(409,r.statusCode());assertTrue(r.body().contains("AMBIENTE_COM_VINCULOS"));
        long outro=id(enviar("POST","/api/ambientes",dados("Historico "+UUID.randomUUID(),"SALA"),true));
        long item=id(enviar("POST",rota(outro)+"/itens","{\"nome\":\"Material\",\"categoria\":\"Teste\",\"formaRegistro\":\"QUANTIDADE\",\"equipamento\":false,\"quantidade\":2,\"status\":\"INATIVO\"}",true));
        assertEquals(200,enviar("DELETE",rota(outro)+"/itens/"+item+"?versao=1",null,true).statusCode());
        assertEquals(409,retirar(outro,1).statusCode());
        assertEquals(200,enviar("GET",rota(outro)+"/itens/"+item+"/historico",null,false).statusCode());
    }
    @Test void responsabilidadeEmOutraSalaBloqueiaRetiradaEConversao() throws Exception {
        item("ATIVO",true);
        assertEquals(409,retirar(setor,1).statusCode());
        var r=enviar("PUT",rota(setor),dados("Tentativa de conversao","SALA"),true);
        assertEquals(409,r.statusCode());assertTrue(r.body().contains("SETOR_RESPONSAVEL"));
        assertEquals(200,enviar("PUT",rota(setor),dados("Setor renomeado "+UUID.randomUUID(),"SETOR"),true).statusCode());
    }
    @Test void alteracaoConcorrenteExigeNovaConfirmacao() throws Exception {
        assertEquals(200,enviar("PUT",rota(sala),dados("Renomeada "+UUID.randomUUID(),"SALA"),true).statusCode());
        var r=retirar(sala,1);assertEquals(409,r.statusCode());assertTrue(r.body().contains("AMBIENTE_ALTERADO"));
        assertEquals(204,retirar(sala,2).statusCode());
    }
    @Test void postgresProtegeMesmoEmAtualizacaoDireta() throws Exception {
        item("INATIVO",true);
        try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE ambientes SET ativo=false WHERE id=?")){
            s.setLong(1,sala);assertEquals("23514",assertThrows(SQLException.class,s::executeUpdate).getSQLState());
            s.setLong(1,setor);assertEquals("23514",assertThrows(SQLException.class,s::executeUpdate).getSQLState());
        }
        try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE ambientes SET tipo='SALA' WHERE id=?")){
            s.setLong(1,setor);assertEquals("23514",assertThrows(SQLException.class,s::executeUpdate).getSQLState());
        }
    }
    @Test void inclusaoConcorrenteNaoPermiteAmbienteInativoComItem() throws Exception {
        try(var c=Connect.conexao()){
            c.setAutoCommit(false);
            try(var s=c.createStatement()){s.execute("SELECT set_config('stocka.usuario_id',(SELECT id::text FROM usuarios WHERE login='admin@stocka.local'),true)");}
            try(var s=c.prepareStatement("INSERT INTO itens(ambiente_id,nome,categoria,forma_registro,equipamento,quantidade,status) VALUES (?,'Concorrente','Teste','INDIVIDUAL',false,1,'ATIVO')")){s.setLong(1,sala);s.executeUpdate();}
            var pendente=CompletableFuture.supplyAsync(()->{try{return retirar(sala,1);}catch(Exception e){throw new RuntimeException(e);}});
            try {Thread.sleep(150);assertFalse(pendente.isDone());c.commit();assertEquals(409,pendente.get(10,TimeUnit.SECONDS).statusCode());}
            finally {c.rollback();}
        }
    }
    @Test void ambienteRetiradoNaoAceitaNovosItensNemAcessos() throws Exception {
        assertEquals(204,retirar(setor,1).statusCode());
        assertEquals(404,enviar("PUT",rota(setor)+"/acessos","{\"usuarios\":[]}",true).statusCode());
        assertEquals(404,enviar("PUT",rota(setor),dados("Nao pode","SETOR"),true).statusCode());
        var r=enviar("POST",rota(setor)+"/itens","{\"nome\":\"Novo\",\"categoria\":\"Teste\",\"formaRegistro\":\"QUANTIDADE\",\"equipamento\":false,\"quantidade\":1,\"status\":\"ATIVO\"}",true);
        assertEquals(404,r.statusCode());
    }
}
