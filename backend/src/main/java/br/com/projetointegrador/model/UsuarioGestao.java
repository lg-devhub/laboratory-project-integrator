package br.com.projetointegrador.model;

// Contrato publico de administracao; nunca inclui senha/hash.
public record UsuarioGestao(long id,String nome,String login,String perfil,boolean ativo,long versao) { }
