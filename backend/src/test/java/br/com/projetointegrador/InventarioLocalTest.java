package br.com.projetointegrador;

import br.com.projetointegrador.database.Connect;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

// Executado SOMENTE no banco descartavel criado por Testar-Inventario.ps1.
@EnabledIfSystemProperty(named="stocka.testesInventario",matches="true")
class InventarioLocalTest {
    private static ConfigurableApplicationContext application;
    private static String base;
    private static long producao;
    private static long sala;
    private HttpClient client;

    @BeforeAll static void iniciar() throws Exception {
        String url=Dotenv.configure().ignoreIfMissing().load().get("DB_URL");
        assertNotNull(url);
        assertTrue(url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):15432/stocka_testes_inventario_[a-f0-9]{12}(\\?.*)?"),
                "Este teste exige um banco temporario exclusivo; nunca use o banco stocka.");
        producao=ambienteId("Producao"); sala=ambienteId("Sala de manutencao");
        application=SpringApplication.run(ApiApplication.class,"--server.port=0");
        base="http://localhost:"+application.getEnvironment().getProperty("local.server.port");
    }
    @AfterAll static void parar() { if (application!=null) application.close(); }
    @BeforeEach void navegador() {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
    }
    private static long ambienteId(String nome) throws Exception {
        try (var c=Connect.conexao();var s=c.prepareStatement("SELECT id FROM ambientes WHERE nome=?")) {
            s.setString(1,nome);try(var r=s.executeQuery()){assertTrue(r.next());return r.getLong(1);}
        }
    }
    private HttpResponse<String> enviar(String metodo,String rota,String json) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(base+rota)).timeout(Duration.ofSeconds(10));
        if (json!=null) request.header("Content-Type","application/json").header("X-STOCKA-Request","true");
        request.method(metodo,json==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json));
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String rota) throws Exception { return enviar("GET",rota,null); }
    private void entrar(String conta) throws Exception {
        assertEquals(200,enviar("POST","/api/auth/login","{\"login\":\""+conta+"@stocka.local\",\"senha\":\"Stocka123\"}").statusCode());
    }
    private String itens(long ambiente) { return "/api/ambientes/"+ambiente+"/itens"; }
    private String lote() {
        return "{\"nome\":\"Parafusos "+UUID.randomUUID()+"\",\"categoria\":\"Fixadores\",\"formaRegistro\":\"QUANTIDADE\",\"equipamento\":false,\"quantidade\":12,\"status\":\"ATIVO\",\"descricao\":\"Lote de teste\"}";
    }
    private String equipamento(String patrimonio) {
        return "{\"nome\":\"Furadeira\",\"categoria\":\"Ferramentas\",\"formaRegistro\":\"INDIVIDUAL\",\"equipamento\":true,\"quantidade\":1,\"status\":\"EM_MANUTENCAO\",\"numeroPatrimonio\":\""+patrimonio+"\",\"setorResponsavelId\":"+producao+"}";
    }
    private long id(HttpResponse<String> resposta) {
        assertEquals(201,resposta.statusCode(),resposta.body());
        var matcher=Pattern.compile("\"id\":(\\d+)").matcher(resposta.body());
        assertTrue(matcher.find());return Long.parseLong(matcher.group(1));
    }

    @Test void exigeSessaoEmTodosOsDadosDoInventario() throws Exception {
        for(String rota:new String[]{"/api/ambientes/"+producao,itens(producao),itens(producao)+"/1/historico",itens(producao)+"/1/foto","/api/itens/setores-responsaveis"})
            assertEquals(401,get(rota).statusCode());
        assertEquals(401,enviar("POST",itens(producao),lote()).statusCode());
    }

    @Test void atribuicaoLimitaUrlDiretaECadastroMesmoComIdentidadeForjada() throws Exception {
        entrar("operador");
        assertEquals(200,get("/api/ambientes/"+producao).statusCode());
        assertEquals(404,get("/api/ambientes/"+sala).statusCode());
        assertEquals(404,get(itens(sala)).statusCode());
        assertEquals(404,enviar("POST",itens(sala)+"?usuarioId=1&perfil=ADMINISTRADOR",lote()).statusCode());
        assertEquals(404,get(itens(sala)+"/1/historico").statusCode());
        assertEquals(404,get(itens(sala)+"/1/foto").statusCode());
        assertEquals(404,get("/api/ambientes/9223372036854775807").statusCode());
        entrar("semacesso");assertEquals(404,get(itens(producao)).statusCode());
    }

    @Test void cadastroDeUsuarioAutorizadoGeraAuditoriaComAutorReal() throws Exception {
        entrar("operador");
        String json=lote().replace("\"categoria\"","\"usuarioId\":1,\"ambienteId\":"+sala+",\"categoria\"");
        long id=id(enviar("POST",itens(producao),json));
        var historico=get(itens(producao)+"/"+id+"/historico");
        assertEquals(200,historico.statusCode());
        assertTrue(historico.body().contains("CADASTRO"));
        assertTrue(historico.body().contains("Operador de teste"));
        assertFalse(historico.body().contains("Administrador de teste"));
        assertFalse(historico.body().contains("senha"));
        assertEquals(404,get(itens(sala)+"/"+id+"/historico").statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("SELECT ambiente_id FROM itens WHERE id=?")){
            s.setLong(1,id);try(var r=s.executeQuery()){r.next();assertEquals(producao,r.getLong(1));}
        }
    }

    @Test void equipamentoTemLocalizacaoSeparadaDaResponsabilidadeSemDuplicarInventario() throws Exception {
        entrar("admin");
        String patrimonio="PAT-"+UUID.randomUUID();
        id(enviar("POST",itens(sala),equipamento(patrimonio)));
        assertTrue(get(itens(sala)).body().contains(patrimonio));
        assertFalse(get(itens(producao)).body().contains(patrimonio));
        var resposta=get(itens(sala));
        assertTrue(resposta.body().contains("\"setorResponsavelNome\":\"Producao\""));
        assertFalse(resposta.body().contains("fotoBase64"));
        entrar("operador");assertEquals(404,get(itens(sala)).statusCode());
    }

    @Test void naoReutilizaPatrimonioMesmoComExclusaoLogica() throws Exception {
        entrar("admin");String patrimonio="PAT-"+UUID.randomUUID();
        long id=id(enviar("POST",itens(sala),equipamento(patrimonio)));
        assertEquals(409,enviar("POST",itens(producao),equipamento(patrimonio.toLowerCase())).statusCode());
        // Estado excluido no banco isolado para conferir preservacao e unicidade.
        try(var c=Connect.conexao();var s=c.prepareStatement("UPDATE itens SET excluido_em=now() WHERE id=?")){
            c.setAutoCommit(false);
            try(var autor=c.createStatement()){autor.execute("SELECT set_config('stocka.usuario_id',(SELECT id::text FROM usuarios WHERE login='admin@stocka.local'),true)");}
            s.setLong(1,id);s.executeUpdate();c.commit();
        }
        assertEquals(409,enviar("POST",itens(producao),equipamento(patrimonio)).statusCode());
        assertFalse(get(itens(sala)).body().contains(patrimonio));
        assertEquals(200,get(itens(sala)+"/"+id+"/historico").statusCode()); // Consulta administrativa dos excluidos.
    }

    @Test void validaObrigatoriosQuantidadeInteiraStatusEVinculos() throws Exception {
        entrar("admin");String valido=lote();
        for(String json:new String[]{"null","{}",valido.replace("\"quantidade\":12","\"quantidade\":0"),
                valido.replace("\"quantidade\":12","\"quantidade\":1.5"),
                valido.replace("\"quantidade\":12","\"quantidade\":2147483648"),
                valido.replace("ATIVO","INVALIDO"),valido.replace("Fixadores","   "),
                valido.replace("QUANTIDADE","INDIVIDUAL"),valido.replace("QUANTIDADE","INVALIDO"),
                equipamento("PAT").replace("\"quantidade\":1","\"quantidade\":2"),
                equipamento("PAT").replace("INDIVIDUAL","QUANTIDADE"),
                equipamento(" "),equipamento("PAT").replace("\"setorResponsavelId\":"+producao,"\"setorResponsavelId\":"+sala),
                equipamento("PAT").replace("\"setorResponsavelId\":"+producao,"\"setorResponsavelId\":9223372036854775807")})
            assertEquals(400,enviar("POST",itens(sala),json).statusCode(),json);
        String individual=valido.replace("QUANTIDADE","INDIVIDUAL").replace("\"quantidade\":12","\"quantidade\":1");
        id(enviar("POST",itens(sala),individual));
    }

    @Test void fotoEValidadaEPodeSerConsultadaSomenteNoAmbienteAutorizado() throws Exception {
        entrar("admin");
        var imagem=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var stream=new ByteArrayOutputStream();
        ImageIO.write(imagem,"png",stream);
        String foto=Base64.getEncoder().encodeToString(stream.toByteArray());
        String json=lote();json=json.substring(0,json.length()-1)+",\"fotoTipo\":\"image/png\",\"fotoBase64\":\""+foto+"\"}";
        var resposta=enviar("POST",itens(sala),json);long id=id(resposta);
        assertTrue(resposta.body().contains("\"temFoto\":true"));
        var imagemResposta=client.send(HttpRequest.newBuilder(URI.create(base+itens(sala)+"/"+id+"/foto")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,imagemResposta.statusCode());assertArrayEquals(stream.toByteArray(),imagemResposta.body());
        assertEquals("image/png",imagemResposta.headers().firstValue("content-type").orElseThrow());
        assertEquals(400,enviar("POST",itens(sala),json.replace(foto,"INVALIDO")).statusCode());
        assertEquals(400,enviar("POST",itens(sala),json.replace("image/png","image/svg+xml")).statusCode());
        assertEquals(400,enviar("POST",itens(sala),json.replace(foto,"A".repeat(2796208))).statusCode());
        entrar("operador");assertEquals(404,get(itens(sala)+"/"+id+"/foto").statusCode());
    }

    @Test void historicoNaoPermiteUpdateDeleteOuTruncateMesmoDiretamenteNoBanco() throws Exception {
        entrar("admin");id(enviar("POST",itens(sala),lote()));
        try(var c=Connect.conexao()){
            for(String sql:new String[]{"UPDATE item_historico SET acao='EDICAO'","DELETE FROM item_historico","TRUNCATE item_historico"}) {
                try(var s=c.createStatement()){
                    var erro=assertThrows(SQLException.class,()->s.execute(sql));assertEquals("23514",erro.getSQLState());
                }
            }
        }
    }

    @Test void falhaDaAuditoriaReverteCadastroInteiro() throws Exception {
        String nome="Sem-autor-"+UUID.randomUUID();
        try(var c=Connect.conexao()){
            c.setAutoCommit(false);
            try(var s=c.prepareStatement("INSERT INTO itens(ambiente_id,nome,categoria,forma_registro,equipamento,quantidade,status) VALUES (?,?,'Teste','QUANTIDADE',false,1,'ATIVO')")){
                s.setLong(1,sala);s.setString(2,nome);
                assertThrows(SQLException.class,s::executeUpdate);c.rollback();
            }
            try(var s=c.prepareStatement("SELECT count(*) FROM itens WHERE nome=?")){
                s.setString(1,nome);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
            } finally { c.rollback(); }
        }
    }

    @Test void revogarAtribuicaoRetiraConsultaECadastroNaMesmaSessao() throws Exception {
        entrar("operador");assertEquals(200,get(itens(producao)).statusCode());
        try(var c=Connect.conexao();var s=c.prepareStatement("DELETE FROM usuario_ambiente WHERE ambiente_id=? AND usuario_id=(SELECT id FROM usuarios WHERE login='operador@stocka.local')")){
            s.setLong(1,producao);s.executeUpdate();
            try {
                assertEquals(404,get(itens(producao)).statusCode());assertEquals(404,enviar("POST",itens(producao),lote()).statusCode());
            } finally {
                try(var restaurar=c.prepareStatement("INSERT INTO usuario_ambiente SELECT id,? FROM usuarios WHERE login='operador@stocka.local'")){
                    restaurar.setLong(1,producao);restaurar.executeUpdate();
                }
            }
        }
    }

    @Test void seletorDeSetoresRetornaApenasMetadadosSemLiberarInventario() throws Exception {
        entrar("semacesso");var lista=get("/api/itens/setores-responsaveis");
        assertEquals(200,lista.statusCode());assertTrue(lista.body().contains("Producao"));
        assertFalse(lista.body().contains("Sala de manutencao"));assertFalse(lista.body().contains("descricao"));
        assertFalse(lista.body().contains("usuario"));assertEquals(404,get(itens(producao)).statusCode());
    }

    private String edicao(String nome,String status,long versao) {
        return "{\"nome\":\""+nome+"\",\"categoria\":\"Fixadores\",\"quantidade\":12,\"status\":\""+status
                +"\",\"descricao\":\"Descricao atualizada\",\"localizacao\":\"Prateleira B\",\"versao\":"+versao+",\"fotoAcao\":\"MANTER\"}";
    }
    private long escalar(String sql,long itemId) throws Exception {
        try(var c=Connect.conexao();var s=c.prepareStatement(sql)){
            s.setLong(1,itemId);try(var r=s.executeQuery()){assertTrue(r.next());return r.getLong(1);}
        }
    }

    @Test void edicaoAtualizaDadosStatusEHistoricoComAutorReal() throws Exception {
        entrar("operador");long id=id(enviar("POST",itens(producao),lote()));
        var resposta=enviar("PUT",itens(producao)+"/"+id,edicao("Parafusos revisados","INATIVO",1));
        assertEquals(200,resposta.statusCode(),resposta.body());
        assertTrue(resposta.body().contains("Parafusos revisados"));
        assertTrue(resposta.body().contains("\"versao\":2"));
        assertTrue(resposta.body().contains("\"localizacao\":\"Prateleira B\""));
        var h=get(itens(producao)+"/"+id+"/historico");
        assertTrue(h.body().contains("\"acao\":\"EDICAO\""));
        assertTrue(h.body().contains("\"statusAnterior\":\"ATIVO\""));
        assertTrue(h.body().contains("\"statusNovo\":\"INATIVO\""));
        assertTrue(h.body().contains("Operador de teste"));
        assertEquals(2,escalar("SELECT count(*) FROM item_historico WHERE item_id=?",id));
    }

    @Test void apenasMudarStatusGeraEventoEspecificoESalvarIgualNaoDuplicaAuditoria() throws Exception {
        entrar("admin");String original=lote();long id=id(enviar("POST",itens(sala),original));
        String json=original.substring(0,original.length()-1)+",\"versao\":1}";
        assertEquals(200,enviar("PUT",itens(sala)+"/"+id,json).statusCode());
        assertEquals(1,escalar("SELECT versao FROM itens WHERE id=?",id));
        assertEquals(1,escalar("SELECT count(*) FROM item_historico WHERE item_id=?",id));
        assertEquals(200,enviar("PUT",itens(sala)+"/"+id,json.replace("ATIVO","INATIVO")).statusCode());
        assertTrue(get(itens(sala)+"/"+id+"/historico").body().contains("\"acao\":\"ALTERACAO_STATUS\""));
        assertEquals(2,escalar("SELECT versao FROM itens WHERE id=?",id));
    }

    @Test void formularioDesatualizadoRecebe409SemSobrescreverPrimeiraEdicao() throws Exception {
        entrar("admin");long id=id(enviar("POST",itens(sala),lote()));String rota=itens(sala)+"/"+id;
        assertEquals(200,enviar("PUT",rota,edicao("Primeira alteracao","ATIVO",1)).statusCode());
        var conflito=enviar("PUT",rota,edicao("Segunda alteracao","INATIVO",1));
        assertEquals(409,conflito.statusCode());assertTrue(conflito.body().contains("ITEM_ALTERADO"));
        assertTrue(conflito.body().contains("\"itemAtual\""));assertTrue(conflito.body().contains("Primeira alteracao"));
        assertFalse(get(rota).body().contains("Segunda alteracao"));
        assertEquals(2,escalar("SELECT count(*) FROM item_historico WHERE item_id=?",id));
        assertEquals(200,enviar("PUT",rota,edicao("Segunda alteracao","INATIVO",2)).statusCode());
        assertEquals(3,escalar("SELECT versao FROM itens WHERE id=?",id));
    }

    @Test void duasRequisicoesConcorrentesNaoPerdemAlteracoes() throws Exception {
        entrar("admin");long id=id(enviar("POST",itens(sala),lote()));String rota=itens(sala)+"/"+id;
        var construtor=HttpRequest.newBuilder(URI.create(base+rota)).header("Content-Type","application/json").header("X-STOCKA-Request","true");
        var a=client.sendAsync(construtor.PUT(HttpRequest.BodyPublishers.ofString(edicao("A","ATIVO",1))).build(),HttpResponse.BodyHandlers.ofString());
        var b=client.sendAsync(construtor.PUT(HttpRequest.BodyPublishers.ofString(edicao("B","ATIVO",1))).build(),HttpResponse.BodyHandlers.ofString());
        var codigos=java.util.List.of(a.get().statusCode(),b.get().statusCode()).stream().sorted().toList();
        assertEquals(java.util.List.of(200,409),codigos);
        assertEquals(2,escalar("SELECT versao FROM itens WHERE id=?",id));
        assertEquals(2,escalar("SELECT count(*) FROM item_historico WHERE item_id=?",id));
    }

    @Test void edicaoRevalidaPermissaoENaoPermiteAlterarIdentidadeOuVinculos() throws Exception {
        entrar("admin");String patrimonio="PAT-"+UUID.randomUUID();long id=id(enviar("POST",itens(sala),equipamento(patrimonio)));
        String rota=itens(sala)+"/"+id;
        assertEquals(400,enviar("PUT",rota,edicao("Equipamento","ATIVO",1)).statusCode());
        String json=edicao("Equipamento revisado","ATIVO",1).replace("\"quantidade\":12","\"quantidade\":1");
        json=json.substring(0,json.length()-1)+",\"ambienteId\":"+producao+",\"setorResponsavelId\":"+sala
                +",\"numeroPatrimonio\":\"FORJADO\",\"equipamento\":false,\"formaRegistro\":\"QUANTIDADE\"}";
        var salvo=enviar("PUT",rota,json);assertEquals(200,salvo.statusCode(),salvo.body());
        assertTrue(salvo.body().contains(patrimonio));assertFalse(salvo.body().contains("FORJADO"));
        assertEquals(sala,escalar("SELECT ambiente_id FROM itens WHERE id=?",id));
        assertEquals(producao,escalar("SELECT setor_responsavel_id FROM itens WHERE id=?",id));
        entrar("operador");assertEquals(404,enviar("PUT",rota,json).statusCode());
        assertEquals(404,get(rota).statusCode());
        entrar("semacesso");assertEquals(404,enviar("PUT",rota,json).statusCode());
        enviar("POST","/api/auth/logout","{}");assertEquals(401,enviar("PUT",rota,json).statusCode());
    }

    @Test void validaEdicaoESemAlterarAuditoriaEmFalhas() throws Exception {
        entrar("admin");long id=id(enviar("POST",itens(sala),lote()));String rota=itens(sala)+"/"+id;
        String valido=edicao("Revisado","ATIVO",1);
        for(String invalido:new String[]{"null","{}",valido.replace("\"versao\":1","\"versao\":0"),
                valido.replace("\"nome\":\"Revisado\"","\"nome\":\" \""),
                valido.replace("\"quantidade\":12","\"quantidade\":1.5"),valido.replace("ATIVO","INVALIDO"),
                valido.replace("MANTER","INVALIDO"),valido.replace("MANTER","SUBSTITUIR")})
            assertEquals(400,enviar("PUT",rota,invalido).statusCode(),invalido);
        assertEquals(1,escalar("SELECT versao FROM itens WHERE id=?",id));
        assertEquals(1,escalar("SELECT count(*) FROM item_historico WHERE item_id=?",id));
        assertEquals(404,enviar("PUT",itens(sala)+"/9223372036854775807",valido).statusCode());
    }

    @Test void fotoPodeSerMantidaSubstituidaERemovidaDuranteEdicao() throws Exception {
        entrar("admin");var stream=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",stream);
        String foto=Base64.getEncoder().encodeToString(stream.toByteArray());
        String original=lote();original=original.substring(0,original.length()-1)+",\"fotoTipo\":\"image/png\",\"fotoBase64\":\""+foto+"\"}";
        long id=id(enviar("POST",itens(sala),original));String rota=itens(sala)+"/"+id;
        assertEquals(200,enviar("PUT",rota,edicao("Com foto","ATIVO",1)).statusCode());
        assertEquals(200,get(rota+"/foto").statusCode());
        String invalida=edicao("Nao gravar","ATIVO",2).replace("MANTER","SUBSTITUIR");
        invalida=invalida.substring(0,invalida.length()-1)+",\"fotoTipo\":\"image/png\",\"fotoBase64\":\"invalida\"}";
        assertEquals(400,enviar("PUT",rota,invalida).statusCode());
        assertEquals(2,escalar("SELECT versao FROM itens WHERE id=?",id));
        var streamNova=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),"png",streamNova);
        String nova=edicao("Foto substituida","ATIVO",2).replace("MANTER","SUBSTITUIR");
        nova=nova.substring(0,nova.length()-1)+",\"fotoTipo\":\"image/png\",\"fotoBase64\":\""+Base64.getEncoder().encodeToString(streamNova.toByteArray())+"\"}";
        assertEquals(200,enviar("PUT",rota,nova).statusCode());
        assertEquals(200,enviar("PUT",rota,edicao("Sem foto","ATIVO",3).replace("MANTER","REMOVER")).statusCode());
        assertEquals(404,get(rota+"/foto").statusCode());
        assertTrue(get(rota).body().contains("\"temFoto\":false"));
    }

    @Test void falhaDeAuditoriaTambemReverteDadosEVersaoNaEdicao() throws Exception {
        entrar("admin");long id=id(enviar("POST",itens(sala),lote()));
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try(var autor=c.createStatement()){autor.execute("SELECT set_config('stocka.usuario_id','9223372036854775807',true)");}
            try(var s=c.prepareStatement("UPDATE itens SET nome='Nao persistir' WHERE id=?")){
                s.setLong(1,id);assertThrows(SQLException.class,s::executeUpdate);
            } finally {c.rollback();}
        }
        assertEquals(1,escalar("SELECT versao FROM itens WHERE id=?",id));
        assertEquals(1,escalar("SELECT count(*) FROM item_historico WHERE item_id=?",id));
        assertFalse(get(itens(sala)+"/"+id).body().contains("Nao persistir"));
    }
}
