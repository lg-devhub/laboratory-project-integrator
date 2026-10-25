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
class OrdemServicoLocalTest {
    private static ConfigurableApplicationContext application;
    private static String base;
    private static long setor,sala;
    private HttpClient client;
    private long equipamento,equipamentoSala,lote;

    @BeforeAll static void iniciar() throws Exception {
        String url=Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url);assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka_testes_inventario_[a-f0-9]{12}(\\?.*)?"));
        try(var c=Connect.conexao();var s=c.createStatement();var r=s.executeQuery("SELECT id,tipo FROM ambientes ORDER BY id")) {
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

    @Test void exigeSessaoEmAberturaListaEDetalhes() throws Exception {
        String rota=rota(setor,equipamento);
        assertEquals(401,enviar("POST",rota,dados()).statusCode());
        assertEquals(401,enviar("GET",rota,null).statusCode());
        assertEquals(401,enviar("GET",rota+"/1",null).statusCode());
    }
    @Test void aberturaUsaSessaoDataEStatusDoServidorSemAlterarEquipamento() throws Exception {
        entrar("operador");String rota=rota(setor,equipamento);
        assertEquals("[]",enviar("GET",rota,null).body());
        String falso=dados().replace("\"prioridade\"","\"solicitanteId\":1,\"equipamentoId\":"+equipamentoSala+",\"status\":\"CONCLUIDA\",\"abertaEm\":\"2000-01-01\",\"prioridade\"");
        var resposta=enviar("POST",rota,falso);long os=id(resposta);
        assertTrue(resposta.body().contains("\"status\":\"ABERTA\""));
        assertTrue(resposta.body().contains("Operador de teste"));
        assertFalse(resposta.body().contains("2000-01-01"));
        assertEquals(200,enviar("GET",resposta.headers().firstValue("Location").orElseThrow(),null).statusCode());
        assertTrue(enviar("GET",rota,null).body().contains("\"id\":"+os));
        assertEquals(201,enviar("POST",rota,dados()).statusCode()); // Nao ha regra de OS unica por equipamento.
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT status,versao,(SELECT count(*) FROM item_historico WHERE item_id=?) FROM itens WHERE id=?")) {
            s.setLong(1,equipamento);s.setLong(2,equipamento);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("ATIVO",r.getString(1));assertEquals(1,r.getLong(2));assertEquals(1,r.getLong(3));}
        }
    }
    @Test void responsabilidadeNaoConcedeAcessoERevogacaoValeNaMesmaSessao() throws Exception {
        entrar("operador");String restrita=rota(sala,equipamentoSala);
        assertEquals(404,enviar("GET",restrita,null).statusCode());assertEquals(404,enviar("POST",restrita,dados()).statusCode());
        String permitida=rota(setor,equipamento);id(enviar("POST",permitida,dados()));
        try(var c=Connect.conexao();var s=c.prepareStatement("DELETE FROM usuario_ambiente WHERE usuario_id=(SELECT id FROM usuarios WHERE login='operador@stocka.local') AND ambiente_id=?")) {
            s.setLong(1,setor);s.executeUpdate();
            try {
                assertEquals(404,enviar("GET",permitida,null).statusCode());assertEquals(404,enviar("POST",permitida,dados()).statusCode());
            } finally {try(var reinserir=c.prepareStatement("INSERT INTO usuario_ambiente(usuario_id,ambiente_id) SELECT id,? FROM usuarios WHERE login='operador@stocka.local'")){reinserir.setLong(1,setor);reinserir.executeUpdate();}}
        }
    }
    @Test void dadosInvalidosNaoCriamOrdens() throws Exception {
        entrar("admin");String rota=rota(setor,equipamento);
        for(String json:new String[]{"null","{}",dados().replace("Ruido no motor"," "),dados().replace("ALTA","URGENTE"),dados().replace("Ruido no motor","x".repeat(2001))})
            assertEquals(400,enviar("POST",rota,json).statusCode());
        assertEquals("[]",enviar("GET",rota,null).body());
        assertEquals(201,enviar("POST",rota,dados().replace("ALTA","BAIXA")).statusCode());
        assertEquals(201,enviar("POST",rota,dados().replace("ALTA","MEDIA")).statusCode());
    }
    @Test void lotesEItensIndividuaisSemEquipamentoNaoRecebemOS() throws Exception {
        entrar("admin");assertEquals(400,enviar("POST",rota(setor,lote),dados()).statusCode());
        assertEquals(400,enviar("GET",rota(setor,lote),null).statusCode());
        long individual=id(enviar("POST","/api/ambientes/"+setor+"/itens","{\"nome\":\"Item simples\",\"categoria\":\"Teste\",\"formaRegistro\":\"INDIVIDUAL\",\"equipamento\":false,\"quantidade\":1,\"status\":\"ATIVO\"}"));
        assertEquals(400,enviar("POST",rota(setor,individual),dados()).statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("INSERT INTO ordens_servico(equipamento_id,solicitante_id,problema,prioridade) SELECT ?,id,'Teste','BAIXA' FROM usuarios WHERE login='admin@stocka.local'")){
            s.setLong(1,lote);assertThrows(SQLException.class,s::executeUpdate);
        }
    }
    @Test void consultaSegueLocalizacaoAtualSemDuplicarOrdem() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(sala,equipamentoSala),dados()));
        assertEquals(404,enviar("GET",rota(setor,equipamento)+"/"+os,null).statusCode());
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try(var s=c.createStatement()){s.execute("SELECT set_config('stocka.usuario_id',(SELECT id::text FROM usuarios WHERE login='admin@stocka.local'),true)");}
            try(var s=c.prepareStatement("UPDATE itens SET ambiente_id=? WHERE id=?")){s.setLong(1,setor);s.setLong(2,equipamentoSala);s.executeUpdate();}
            c.commit();
        }
        assertEquals(404,enviar("GET",rota(sala,equipamentoSala)+"/"+os,null).statusCode());
        entrar("operador");assertEquals(200,enviar("GET",rota(setor,equipamentoSala)+"/"+os,null).statusCode());
        assertEquals(404,enviar("GET",rota(setor,Long.MAX_VALUE),null).statusCode());
    }

    @Test void cicloCompletoPreservaAberturaAutoriaECondicaoDoEquipamento() throws Exception {
        entrar("operador");long os=id(enviar("POST",rota(setor,equipamento),dados()));String caminho=rota(setor,equipamento)+"/"+os;
        var iniciar=enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null));assertEquals(200,iniciar.statusCode());
        assertTrue(iniciar.body().contains("\"versao\":2"));assertTrue(iniciar.body().contains("\"fechadaEm\":null"));
        var editar=enviar("PUT",caminho,edicao("EM_ANDAMENTO",2,null));assertEquals(200,editar.statusCode());
        assertTrue(editar.body().contains("\"versao\":2")); // Sem mudanca efetiva.
        String json=edicao("CONCLUIDA",2,"Motor revisado e testado.").replace("\"prioridade\"","\"solicitanteId\":1,\"equipamentoId\":"+equipamentoSala+",\"fechadaEm\":\"2000-01-01T00:00:00Z\",\"abertaEm\":\"2000-01-01\",\"prioridade\"");
        var concluida=enviar("PUT",caminho,json);assertEquals(200,concluida.statusCode(),concluida.body());
        assertTrue(concluida.body().contains("\"status\":\"CONCLUIDA\""));assertTrue(concluida.body().contains("Motor revisado e testado."));
        assertTrue(concluida.body().contains("Operador de teste"));assertFalse(concluida.body().contains("2000-01-01"));
        assertTrue(concluida.body().contains("\"versao\":3"));
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT o.fechada_em>=o.aberta_em,i.status,i.versao,(SELECT count(*) FROM item_historico WHERE item_id=i.id) FROM ordens_servico o JOIN itens i ON i.id=o.equipamento_id WHERE o.id=?")) {
            s.setLong(1,os);try(var r=s.executeQuery()){assertTrue(r.next());assertTrue(r.getBoolean(1));assertEquals("ATIVO",r.getString(2));assertEquals(1,r.getLong(3));assertEquals(1,r.getLong(4));}
        }
    }
    @Test void rejeitaSaltosRetornosEEncerramentoSemAtendimento() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));String caminho=rota(setor,equipamento)+"/"+os;
        assertEquals(400,enviar("PUT",caminho,edicao("CONCLUIDA",1,"Atendido")).statusCode());
        assertEquals(400,enviar("PUT",caminho,edicao("INVALIDO",1,null)).statusCode());
        assertEquals(400,enviar("PUT",caminho,edicao("ABERTA",1,"Atendimento antecipado")).statusCode());
        assertEquals(400,enviar("PUT",caminho,"{}").statusCode());
        assertEquals(400,enviar("PUT",caminho,edicao("ABERTA",0,null)).statusCode());
        assertEquals(200,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(400,enviar("PUT",caminho,edicao("ABERTA",2,null)).statusCode());
        for(String texto:new String[]{null," ","x".repeat(2001)})assertEquals(400,enviar("PUT",caminho,edicao("CONCLUIDA",2,texto)).statusCode());
        assertTrue(enviar("GET",caminho,null).body().contains("\"versao\":2"));
        assertTrue(enviar("GET",caminho,null).body().contains("\"fechadaEm\":null"));
    }
    @Test void concluidaFicaImutavelNaApiENoBanco() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));String caminho=rota(setor,equipamento)+"/"+os;
        assertEquals(200,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(200,enviar("PUT",caminho,edicao("CONCLUIDA",2,"Ajuste concluido")).statusCode());
        String antes=enviar("GET",caminho,null).body();
        var bloqueio=enviar("PUT",caminho,edicao("CONCLUIDA",3,"Outro atendimento"));
        assertEquals(409,bloqueio.statusCode());assertTrue(bloqueio.body().contains("OS_CONCLUIDA"));
        try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE ordens_servico SET problema='Trocar texto' WHERE id=?")) {
            s.setLong(1,os);assertThrows(SQLException.class,s::executeUpdate);
        }
        assertEquals(antes,enviar("GET",caminho,null).body());
        assertTrue(enviar("GET",rota(setor,equipamento),null).body().contains("Ajuste concluido"));
    }
    @Test void conflitosDeVersaoEGravacoesSimultaneasNaoSobrescrevemOS() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));String caminho=rota(setor,equipamento)+"/"+os;
        assertEquals(200,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
        var antiga=enviar("PUT",caminho,edicao("ABERTA",1,null));assertEquals(409,antiga.statusCode());
        assertTrue(antiga.body().contains("OS_ALTERADA"));assertTrue(antiga.body().contains("ordemAtual"));
        var a=HttpRequest.newBuilder(URI.create(base+caminho)).header("Content-Type","application/json").header("X-STOCKA-Request","true").PUT(HttpRequest.BodyPublishers.ofString(edicao("EM_ANDAMENTO",2,null).replace("Revisar motor","Edicao A"))).build();
        var b=HttpRequest.newBuilder(URI.create(base+caminho)).header("Content-Type","application/json").header("X-STOCKA-Request","true").PUT(HttpRequest.BodyPublishers.ofString(edicao("EM_ANDAMENTO",2,null).replace("Revisar motor","Edicao B"))).build();
        var ra=client.sendAsync(a,HttpResponse.BodyHandlers.ofString());var rb=client.sendAsync(b,HttpResponse.BodyHandlers.ofString());
        assertEquals(java.util.Set.of(200,409),java.util.Set.of(ra.get().statusCode(),rb.get().statusCode()));
        assertTrue(enviar("GET",caminho,null).body().contains("\"versao\":3"));
    }
    @Test void atualizacaoExigeSessaoEAtribuicaoAoAmbienteAtual() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(sala,equipamentoSala),dados()));String caminho=rota(sala,equipamentoSala)+"/"+os;
        assertEquals(404,enviar("PUT",rota(setor,equipamento)+"/"+os,edicao("EM_ANDAMENTO",1,null)).statusCode());
        entrar("operador");assertEquals(404,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
        entrar("semacesso");assertEquals(404,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
        enviar("POST","/api/auth/logout","{}");assertEquals(401,enviar("PUT",caminho,edicao("EM_ANDAMENTO",1,null)).statusCode());
    }
    @Test void bancoBloqueiaTransicoesInvalidasETrocaDeIdentidade() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));
        try(var c=Connect.conexao()) {
            for(String alteracao:new String[]{"status='CONCLUIDA',descricao_atendimento='Pular etapa'","equipamento_id="+equipamentoSala,"aberta_em=TIMESTAMPTZ '2000-01-01'"}) {
                try(var s=c.prepareStatement("UPDATE ordens_servico SET "+alteracao+" WHERE id=?")){s.setLong(1,os);assertThrows(SQLException.class,s::executeUpdate);}
            }
        }
        assertTrue(enviar("GET",rota(setor,equipamento)+"/"+os,null).body().contains("\"versao\":1"));
    }
}
