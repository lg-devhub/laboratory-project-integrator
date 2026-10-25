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
class ManutencaoLocalTest {
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

    private String manutencoes(long ambiente,long item){return "/api/ambientes/"+ambiente+"/itens/"+item+"/manutencoes";}
    private String servico(){return "{\"dataRealizacao\":\"2026-10-24\",\"descricaoServico\":\" Revisao do motor \",\"responsavelTecnico\":\" Tecnico externo \",\"ordemServicoId\":null}";}
    private String resumo(long ambiente){return "/api/ambientes/"+ambiente+"/resumo-os";}
    private String fase(String status,int versao,String atendimento) {
        return "{\"problema\":\"Ruido no motor\",\"prioridade\":\"ALTA\",\"status\":\""+status+"\",\"versao\":"+versao+",\"descricaoAtendimento\":"+(atendimento==null?"null":"\""+atendimento+"\"")+"}";
    }

    @Test void exigeSessaoParaManutencaoEResumoDeOS() throws Exception {
        String caminho=manutencoes(setor,equipamento);
        assertEquals(401,enviar("GET",caminho,null).statusCode());
        assertEquals(401,enviar("GET",caminho+"/1",null).statusCode());
        assertEquals(401,enviar("POST",caminho,servico()).statusCode());
        assertEquals(401,enviar("GET",resumo(setor),null).statusCode());
    }
    @Test void registroSemOSUsaAutorDaSessaoEDataDeLancamentoAutomatica() throws Exception {
        entrar("operador");String caminho=manutencoes(setor,equipamento);
        assertEquals("[]",enviar("GET",caminho,null).body());
        String json=servico().replace("\"ordemServicoId\"","\"usuarioId\":1,\"equipamentoId\":"+equipamentoSala+",\"registradoEm\":\"2000-01-01\",\"ordemServicoId\"");
        var resposta=enviar("POST",caminho,json);long manutencao=id(resposta);
        assertTrue(resposta.body().contains("Operador de teste"));assertTrue(resposta.body().contains("\"ordemServicoId\":null"));
        assertTrue(resposta.body().contains("\"dataRealizacao\":\"2026-10-24\""));assertFalse(resposta.body().contains("2000-01-01"));
        assertTrue(resposta.body().contains("\"responsavelTecnico\":\"Tecnico externo\""));
        assertEquals(200,enviar("GET",resposta.headers().firstValue("Location").orElseThrow(),null).statusCode());
        assertTrue(enviar("GET",caminho,null).body().contains("\"id\":"+manutencao));
        entrar("admin");assertEquals(200,enviar("GET",caminho+"/"+manutencao,null).statusCode());
    }
    @Test void vinculoAceitaOSAbertasEConcluidasSemAlterarOSOuEquipamento() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(setor,equipamento),dados()));
        String caminho=rota(setor,equipamento)+"/"+os;
        id(enviar("POST",manutencoes(setor,equipamento),servico().replace(":null",":"+os)));
        assertEquals(200,enviar("PUT",caminho,fase("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(200,enviar("PUT",caminho,fase("CONCLUIDA",2,"Atendimento concluido")).statusCode());
        String antes=enviar("GET",caminho,null).body();
        id(enviar("POST",manutencoes(setor,equipamento),servico().replace(":null",":"+os)));
        assertEquals(antes,enviar("GET",caminho,null).body());
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT status,versao,(SELECT count(*) FROM item_historico WHERE item_id=?) FROM itens WHERE id=?")) {
            s.setLong(1,equipamento);s.setLong(2,equipamento);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("ATIVO",r.getString(1));assertEquals(1,r.getLong(2));assertEquals(1,r.getLong(3));}
        }
    }
    @Test void responsabilidadeNaoConcedeAcessoERevogacaoValeSemNovoLogin() throws Exception {
        entrar("admin");long m=id(enviar("POST",manutencoes(sala,equipamentoSala),servico()));
        entrar("operador");
        assertEquals(404,enviar("GET",manutencoes(sala,equipamentoSala),null).statusCode());
        assertEquals(404,enviar("GET",manutencoes(sala,equipamentoSala)+"/"+m,null).statusCode());
        assertEquals(404,enviar("POST",manutencoes(sala,equipamentoSala),servico()).statusCode());
        assertEquals(404,enviar("GET",resumo(sala),null).statusCode());
        id(enviar("POST",manutencoes(setor,equipamento),servico()));
        try(var c=Connect.conexao();var s=c.prepareStatement("DELETE FROM usuario_ambiente WHERE usuario_id=(SELECT id FROM usuarios WHERE login='operador@stocka.local') AND ambiente_id=?")) {
            s.setLong(1,setor);s.executeUpdate();
            try {
                assertEquals(404,enviar("GET",manutencoes(setor,equipamento),null).statusCode());
                assertEquals(404,enviar("POST",manutencoes(setor,equipamento),servico()).statusCode());
                assertEquals(404,enviar("GET",resumo(setor),null).statusCode());
            }finally{try(var r=c.prepareStatement("INSERT INTO usuario_ambiente(usuario_id,ambiente_id) SELECT id,? FROM usuarios WHERE login='operador@stocka.local'")){r.setLong(1,setor);r.executeUpdate();}}
        }
    }
    @Test void validacaoNaoDeixaRegistrosParciais() throws Exception {
        entrar("admin");String caminho=manutencoes(setor,equipamento);
        for(String json:new String[]{"null","{}",servico().replace("2026-10-24","2026-02-30"),servico().replace("Revisao do motor"," "),
            servico().replace("Tecnico externo"," "),servico().replace("Revisao do motor","x".repeat(2001)),
            servico().replace("Tecnico externo","x".repeat(121)),servico().replace(":null",":0"),servico().replace(":null",":1.5")})
            assertEquals(400,enviar("POST",caminho,json).statusCode(),json);
        assertEquals("[]",enviar("GET",caminho,null).body());
    }
    @Test void OSDeOutroEquipamentoEhRejeitadaTambemPeloBanco() throws Exception {
        entrar("admin");long os=id(enviar("POST",rota(sala,equipamentoSala),dados()));
        assertEquals(400,enviar("POST",manutencoes(setor,equipamento),servico().replace(":null",":"+os)).statusCode());
        assertEquals(400,enviar("POST",manutencoes(setor,equipamento),servico().replace(":null",":"+Long.MAX_VALUE)).statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("INSERT INTO manutencoes(equipamento_id,ordem_servico_id,data_realizacao,descricao_servico,responsavel_tecnico,usuario_id) SELECT ?,?,DATE '2026-10-24','Teste','Tecnico',id FROM usuarios WHERE login='admin@stocka.local'")) {
            s.setLong(1,equipamento);s.setLong(2,os);assertThrows(SQLException.class,s::executeUpdate);
        }
        assertEquals("[]",enviar("GET",manutencoes(setor,equipamento),null).body());
    }
    @Test void lotesEItensSemPatrimonioNaoRecebemManutencao() throws Exception {
        entrar("admin");
        assertEquals(400,enviar("POST",manutencoes(setor,lote),servico()).statusCode());
        assertEquals(400,enviar("GET",manutencoes(setor,lote),null).statusCode());
        long simples=id(enviar("POST","/api/ambientes/"+setor+"/itens","{\"nome\":\"Item simples\",\"categoria\":\"Teste\",\"formaRegistro\":\"INDIVIDUAL\",\"equipamento\":false,\"quantidade\":1,\"status\":\"ATIVO\"}"));
        assertEquals(400,enviar("POST",manutencoes(setor,simples),servico()).statusCode());
        assertEquals(404,enviar("GET",manutencoes(setor,Long.MAX_VALUE),null).statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("INSERT INTO manutencoes(equipamento_id,data_realizacao,descricao_servico,responsavel_tecnico,usuario_id) SELECT ?,DATE '2026-10-24','Teste','Tecnico',id FROM usuarios WHERE login='admin@stocka.local'")) {
            s.setLong(1,lote);assertThrows(SQLException.class,s::executeUpdate);
        }
    }
    @Test void resumoContaFasesPorEquipamentoSomenteNaLocalizacaoAtual() throws Exception {
        entrar("admin");
        id(enviar("POST",rota(setor,equipamento),dados()));
        long andamento=id(enviar("POST",rota(setor,equipamento),dados()));
        long concluida=id(enviar("POST",rota(setor,equipamento),dados()));
        assertEquals(200,enviar("PUT",rota(setor,equipamento)+"/"+andamento,fase("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(200,enviar("PUT",rota(setor,equipamento)+"/"+concluida,fase("EM_ANDAMENTO",1,null)).statusCode());
        assertEquals(200,enviar("PUT",rota(setor,equipamento)+"/"+concluida,fase("CONCLUIDA",2,"Atendido")).statusCode());
        id(enviar("POST",rota(sala,equipamentoSala),dados()));
        var resposta=enviar("GET",resumo(setor),null);assertEquals(200,resposta.statusCode());
        assertTrue(resposta.body().contains("{\"itemId\":"+equipamento+",\"abertas\":1,\"emAndamento\":1,\"concluidas\":1}"));
        assertFalse(resposta.body().contains("\"itemId\":"+equipamentoSala+","));
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try(var s=c.createStatement()){s.execute("SELECT set_config('stocka.usuario_id',(SELECT id::text FROM usuarios WHERE login='admin@stocka.local'),true)");}
            try(var s=c.prepareStatement("UPDATE itens SET ambiente_id=? WHERE id=?")){s.setLong(1,setor);s.setLong(2,equipamentoSala);s.executeUpdate();}
            c.commit();
        }
        assertFalse(enviar("GET",resumo(sala),null).body().contains("\"itemId\":"+equipamentoSala+","));
        assertTrue(enviar("GET",resumo(setor),null).body().contains("\"itemId\":"+equipamentoSala+","));
    }
}
