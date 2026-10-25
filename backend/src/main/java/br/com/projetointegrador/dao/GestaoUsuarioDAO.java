package br.com.projetointegrador.dao;

import br.com.projetointegrador.database.Connect;
import br.com.projetointegrador.model.UsuarioDados;
import br.com.projetointegrador.model.UsuarioGestao;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.*;

public class GestaoUsuarioDAO {
    private static final String SELECT="SELECT id,nome,login,perfil,ativo,versao FROM public.usuarios";
    public static class SemPermissao extends RuntimeException { }
    public static class Conflito extends RuntimeException {
        public final UsuarioGestao atual;
        public Conflito(UsuarioGestao atual){this.atual=atual;}
    }
    private UsuarioGestao ler(ResultSet r) throws SQLException {
        return new UsuarioGestao(r.getLong("id"),r.getString("nome"),r.getString("login"),r.getString("perfil"),r.getBoolean("ativo"),r.getLong("versao"));
    }
    private void exigirAdmin(Connection c,long autor) throws SQLException {
        try(var s=c.prepareStatement("SELECT id FROM public.usuarios WHERE id=? AND ativo AND perfil='ADMINISTRADOR' FOR SHARE")) {
            s.setLong(1,autor);try(var r=s.executeQuery()){if(!r.next())throw new SemPermissao();}
        }
    }
    public List<UsuarioGestao> listar(long autor) throws SQLException {
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                exigirAdmin(c,autor);List<UsuarioGestao> lista=new ArrayList<>();
                try(var s=c.prepareStatement(SELECT+" ORDER BY ativo DESC,lower(nome),id");var r=s.executeQuery()){while(r.next())lista.add(ler(r));}
                c.commit();return lista;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    public Optional<UsuarioGestao> salvar(long autor,Long id,UsuarioDados dados,boolean desativar,Long versao) throws SQLException {
        String hash=dados!=null && dados.senha()!=null?BCrypt.hashpw(dados.senha(),BCrypt.gensalt()):null;
        try(var c=Connect.conexao()) {
            c.setAutoCommit(false);
            try {
                // Pequeno cadastro de contas: serializa escritas administrativas antes de ler a identidade.
                try(var s=c.createStatement()){s.execute("LOCK TABLE public.usuarios IN SHARE ROW EXCLUSIVE MODE");}
                exigirAdmin(c,autor);
                if(id!=null) {
                    UsuarioGestao atual;
                    try(var s=c.prepareStatement(SELECT+" WHERE id=? FOR UPDATE")){
                        s.setLong(1,id);try(var r=s.executeQuery()){if(!r.next()){c.rollback();return Optional.empty();}atual=ler(r);}
                    }
                    if(atual.versao()!=versao)throw new Conflito(atual);
                    if(desativar) {
                        if(!atual.ativo())throw new IllegalArgumentException("Esta conta ja esta desativada.");
                        try(var s=c.prepareStatement("UPDATE public.usuarios SET ativo=false WHERE id=?")){s.setLong(1,id);s.executeUpdate();}
                    }else{
                        try(var s=c.prepareStatement("UPDATE public.usuarios SET nome=?,login=?,perfil=?,senha_hash=COALESCE(?,senha_hash) WHERE id=?")){
                            s.setString(1,dados.nome());s.setString(2,dados.login());s.setString(3,dados.perfil());s.setString(4,hash);s.setLong(5,id);s.executeUpdate();
                        }
                    }
                }else{
                    try(var s=c.prepareStatement("INSERT INTO public.usuarios(nome,login,perfil,senha_hash) VALUES (?,?,?,?) RETURNING id")){
                        s.setString(1,dados.nome());s.setString(2,dados.login());s.setString(3,dados.perfil());s.setString(4,hash);
                        try(var r=s.executeQuery()){r.next();id=r.getLong(1);}
                    }
                }
                UsuarioGestao salva;
                try(var s=c.prepareStatement(SELECT+" WHERE id=?")){s.setLong(1,id);try(var r=s.executeQuery()){r.next();salva=ler(r);}}
                c.commit();return Optional.of(salva);
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
}
