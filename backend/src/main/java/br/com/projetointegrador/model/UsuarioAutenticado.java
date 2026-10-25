package br.com.projetointegrador.model;

// Record e uma classe simples de dados imutaveis. Nao inclui senha nem hash.
public record UsuarioAutenticado(long id, String nome, String login, String perfil) {
}
