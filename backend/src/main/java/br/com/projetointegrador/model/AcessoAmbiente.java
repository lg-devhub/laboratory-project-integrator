package br.com.projetointegrador.model;

// Somente administradores recebem esta lista; nenhum hash e retornado.
public record AcessoAmbiente(long id, String nome, String login, boolean ativo, boolean atribuido) { }
