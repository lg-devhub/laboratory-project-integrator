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
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="stocka.testesInventario",matches="true")
class ExclusaoItemLocalTest {
    private static ConfigurableApplicationContext application;
    private static String base;
    private static long setor,sala;
    private HttpClient client;
    private long equipamento,equipamentoSala,lote;

    @BeforeAll static void iniciar() throws Exception {
        String url=Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url);assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka_testes_inventario_[a-f0-9]{12}(\\?.*)?"));
        try(var c=Connect.conexao();var s=c.createStatement();var r=s.executeQuery("SELECT id,tipo FROM ambientes WHERE nome IN ('Producao','Sala de manutencao') ORDER BY id")) {
            while(r.next()){if(r.getString(2).equals("SETOR"))setor=r.getLong(1);else sala=r.getLong(1);}
        }
        application=SpringApplication.run(ApiApplication.class,"--server.port=0");
        base="http://localhost:"+application.getEnvironment().getProperty("local.server.port");
    }
    @AfterAll static void parar(){if(application!=null)application.close();}
    @BeforeEach void preparar() throws Exception {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        entrar("admin");equipamento=criarEquipamento(setor);equipamentoSala=criarEquipamento(sala);
        lote=id(enviar("POST","/api/ambientes/"+setor+"/itens","{\"nome\":\"Lote OS\",\"categoria\":\"Material\",\"formaRegistro\":\"QUANTIDADE\",\"equipamento\":false,\"quantidade\":4,\"status\":\"ATIVO\"}"));
        enviar("POST","/api/auth/logout","{}");
    }
    private HttpResponse<String> enviar(String metodo,String rota,String json) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+rota)).timeout(Duration.ofSeconds(10));
        if(metodo.equals("DELETE"))b.header("X-STOCKA-Request","true");
        if(json!=null)b.header("Content-Type","application/json").header("X-STOCKA-Request","true");
        return client.send(b.method(metodo,json==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private void entrar(String login) throws Exception {assertEquals(200,enviar("POST","/api/auth/login","{\"login\":\""+login+"@stocka.local\",\"senha\":\"Stocka123\"}").statusCode());}
    private long id(HttpResponse<String> r) {assertEquals(201,r.statusCode(),r.body());var m=Pattern.compile("\"id\":(\\d+)").matcher(r.body());assertTrue(m.find());return Long.parseLong(m.group(1));}
    private long criarEquipamento(long ambiente) throws Exception {
        return id(enviar("POST","/api/ambientes/"+ambiente+"/itens","{\"nome\":\"Equipamento OS\",\"categoria\":\"Teste\",\"formaRegistro\":\"INDIVIDUAL\",\"equipamento\":true,\"quantidade\":1,\"status\":\"ATIVO\",\"numeroPatrimonio\":\"OS-"+UUID.randomUUID()+"\",\"setorResponsavelId\":"+setor+"}"));
    }
    private String rota(long ambiente,long item){return "/api/ambientes/"+ambiente+"/itens/"+item+"/ordens-servico";}
    private String dados(){return "{\"problema\":\" Ruido no motor \",\"prioridade\":\"ALTA\"}";}
    private String edicao(String status,int versao,String atendimento) {
        return "{\"problema\":\"Revisar motor\",\"prioridade\":\"MEDIA\",\"status\":\""+status+"\",\"versao\":"+versao+",\"descricaoAtendimento\":"+(atendimento==null?"null":"\""+atendimento+"\"")+"}";
    }

    private String item(long ambiente,long id){return "/api/ambientes/"+ambiente+"/itens/"+id;}
    private HttpResponse<String> excluir(long ambiente,long id,int versao)throws Exception{return enviar("DELETE",item(ambiente,id)+"?versao="+versao,null);}

    @Test void exigeSessaoEAcessoNaoBastaConhecerId() throws Exception {
        assertEquals(401,excluir(setor,lote,1).statusCode());
        entrar("operador");assertEquals(404,excluir(sala,equipamentoSala,1).statusCode());
        assertEquals(403,enviar("GET","/api/ambientes/"+setor+"/itens?excluidos=true",null).statusCode());
        entrar("semacesso");assertEquals(404,excluir(setor,lote,1).statusCode());
    }
    @Test void usuarioAtribuidoExcluiLoteComAuditoriaEAdministradorConsulta() throws Exception {
        entrar("operador");var resposta=excluir(setor,lote,1);assertEquals(200,resposta.statusCode(),resposta.body());
        assertTrue(resposta.body().contains("\"versao\":2"));assertFalse(resposta.body().contains("\"excluidoEm\":null"));
        assertEquals(404,enviar("GET",item(setor,lote),null).statusCode());
        assertEquals(404,enviar("GET",item(setor,lote)+"/historico",null).statusCode());
        entrar("admin");var historico=enviar("GET",item(setor,lote)+"/historico",null);assertEquals(200,historico.statusCode());
        assertTrue(historico.body().contains("EXCLUSAO"));assertTrue(historico.body().contains("Operador de teste"));
        assertTrue(enviar("GET","/api/ambientes/"+setor+"/itens?excluidos=true",null).body().contains("\"id\":"+lote+","));
        assertFalse(enviar("GET","/api/ambientes/"+setor+"/itens",null).body().contains("\"id\":"+lote+","));
    }
    @Test void fotosSaoPreservadasSomenteParaConsultaAdministrativa() throws Exception {
        entrar("admin");var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        String foto=java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
        long id=id(enviar("POST","/api/ambientes/"+setor+"/itens","{\"nome\":\"Fotografado\",\"categoria\":\"Teste\",\"formaRegistro\":\"INDIVIDUAL\",\"equipamento\":false,\"quantidade\":1,\"status\":\"ATIVO\",\"fotoTipo\":\"image/png\",\"fotoBase64\":\""+foto+"\"}"));
        assertEquals(200,excluir(setor,id,1).statusCode());
        assertEquals(200,enviar("GET",item(setor,id)+"/foto",null).statusCode());
        entrar("operador");assertEquals(404,enviar("GET",item(setor,id)+"/foto",null).statusCode());
    }
    @Test void OSPendenteBloqueiaExclusaoTambemNoPostgreSQL() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));
        var aberta=excluir(setor,equipamento,1);assertEquals(409,aberta.statusCode());assertTrue(aberta.body().contains("OS_PENDENTE"));
        assertEquals(200,enviar("PUT",rota(setor,equipamento)+"/"+os,edicao("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(409,excluir(setor,equipamento,1).statusCode());
        try(var c=Connect.conexao()){
            c.setAutoCommit(false);
            try(var s=c.createStatement()){s.execute("SELECT set_config('stocka.usuario_id',(SELECT id::text FROM usuarios WHERE login='admin@stocka.local'),true)");}
            try(var s=c.prepareStatement("UPDATE itens SET excluido_em=now() WHERE id=?")){s.setLong(1,equipamento);assertThrows(SQLException.class,s::executeUpdate);}
            finally{c.rollback();}
        }
        assertTrue(enviar("GET",item(setor,equipamento),null).body().contains("\"excluidoEm\":null"));
    }
    @Test void concluidaEManutencaoContinuamConsultaveisSemPermitirNovasEscritas() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));String caminho=rota(setor,equipamento)+"/"+os;
        assertEquals(200,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(200,enviar("PUT",caminho,edicao("CONCLUIDA",2,"Concluido")).statusCode());
        String manutencao="{\"dataRealizacao\":\"2026-10-24\",\"descricaoServico\":\"Revisao\",\"responsavelTecnico\":\"Tecnico\",\"ordemServicoId\":"+os+"}";
        id(enviar("POST",item(setor,equipamento)+"/manutencoes",manutencao));
        assertEquals(200,excluir(setor,equipamento,1).statusCode());
        assertEquals(200,enviar("GET",caminho,null).statusCode());
        assertTrue(enviar("GET",item(setor,equipamento)+"/manutencoes",null).body().contains("Revisao"));
        assertEquals(404,enviar("POST",rota(setor,equipamento),dados()).statusCode());
        assertEquals(404,enviar("POST",item(setor,equipamento)+"/manutencoes",manutencao).statusCode());
        assertEquals(404,enviar("PUT",item(setor,equipamento),"{\"versao\":2}").statusCode());
        entrar("operador");assertEquals(404,enviar("GET",caminho,null).statusCode());
        assertEquals(404,enviar("GET",item(setor,equipamento)+"/manutencoes",null).statusCode());
        entrar("admin");
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT numero_patrimonio FROM itens WHERE id=?")) {
            s.setLong(1,equipamento);try(var r=s.executeQuery()){
                assertTrue(r.next());String patrimonio=r.getString(1);
                var duplicado=enviar("POST","/api/ambientes/"+setor+"/itens","{\"nome\":\"Duplicado\",\"categoria\":\"Teste\",\"formaRegistro\":\"INDIVIDUAL\",\"equipamento\":true,\"quantidade\":1,\"status\":\"ATIVO\",\"numeroPatrimonio\":\""+patrimonio+"\",\"setorResponsavelId\":"+setor+"}");
                assertEquals(409,duplicado.statusCode());
            }
        }
    }
    @Test void versaoDesatualizadaNaoExcluiEARepeticaoNaoDuplicaAuditoria() throws Exception {
        entrar("admin");
        assertEquals(200,enviar("PUT",item(setor,lote),"{\"nome\":\"Revisado\",\"categoria\":\"Teste\",\"quantidade\":4,\"status\":\"ATIVO\",\"versao\":1}").statusCode());
        var antiga=excluir(setor,lote,1);assertEquals(409,antiga.statusCode());assertTrue(antiga.body().contains("ITEM_ALTERADO"));
        assertEquals(400,excluir(setor,lote,0).statusCode());
        assertEquals(200,excluir(setor,lote,2).statusCode());assertEquals(404,excluir(setor,lote,2).statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT count(*) FROM item_historico WHERE item_id=? AND acao='EXCLUSAO'")){
            s.setLong(1,lote);try(var r=s.executeQuery()){r.next();assertEquals(1,r.getLong(1));}
        }
    }
    @Test void falhaNaAuditoriaReverteExclusaoEVersao() throws Exception {
        entrar("admin");
        try(var c=Connect.conexao()){
            c.setAutoCommit(false);
            try(var s=c.createStatement()){s.execute("SELECT set_config('stocka.usuario_id','9223372036854775807',true)");}
            try(var s=c.prepareStatement("UPDATE itens SET excluido_em=now() WHERE id=?")){s.setLong(1,lote);assertThrows(SQLException.class,s::executeUpdate);}
            finally{c.rollback();}
        }
        var atual=enviar("GET",item(setor,lote),null);assertTrue(atual.body().contains("\"versao\":1"));assertTrue(atual.body().contains("\"excluidoEm\":null"));
    }
    @Test void aberturaDeOSConcorrenteComExclusaoNaoDeixaOSPendenteExcluida() throws Exception {
        entrar("admin");
        var abrir=HttpRequest.newBuilder(URI.create(base+rota(setor,equipamento))).header("Content-Type","application/json").header("X-STOCKA-Request","true").POST(HttpRequest.BodyPublishers.ofString(dados())).build();
        var excluir=HttpRequest.newBuilder(URI.create(base+item(setor,equipamento)+"?versao=1")).header("X-STOCKA-Request","true").DELETE().build();
        var a=client.sendAsync(abrir,HttpResponse.BodyHandlers.ofString());var b=client.sendAsync(excluir,HttpResponse.BodyHandlers.ofString());
        int statusA=a.get().statusCode(),statusB=b.get().statusCode();
        assertTrue((statusA==201 && statusB==409)||(statusA==404 && statusB==200),statusA+"/"+statusB);
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT count(*) FROM itens i JOIN ordens_servico o ON o.equipamento_id=i.id WHERE i.id=? AND i.excluido_em IS NOT NULL AND o.status IN ('ABERTA','EM_ANDAMENTO')")){
            s.setLong(1,equipamento);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getLong(1));}
        }
    }
}
