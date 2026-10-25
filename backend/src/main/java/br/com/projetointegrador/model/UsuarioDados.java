package br.com.projetointegrador.model;

import br.com.projetointegrador.service.AutenticacaoService;
import java.util.List;

public record UsuarioDados(String nome,String login,String perfil,String senha,Long versao) {
    public UsuarioDados validar(boolean cadastro) {
        if(nome==null || nome.isBlank() || nome.strip().length()>120 || nome.indexOf('\0')>=0)
            throw new IllegalArgumentException("Informe o nome em ate 120 caracteres.");
        if(login==null || login.isBlank() || login.strip().length()>254 || login.indexOf('\0')>=0)
            throw new IllegalArgumentException("Informe o e-mail/login em ate 254 caracteres.");
        if(perfil==null || !List.of("ADMINISTRADOR","USUARIO").contains(perfil))
            throw new IllegalArgumentException("Selecione Administrador ou Usuario.");
        String novaSenha=senha==null || senha.isEmpty()?null:senha;
        if(cadastro || novaSenha!=null)AutenticacaoService.validarNovaSenha(novaSenha);
        if(!cadastro && (versao==null || versao<=0))throw new IllegalArgumentException("Informe a versao atual da conta.");
        return new UsuarioDados(nome.strip(),login.strip(),perfil,novaSenha,versao);
    }
}
