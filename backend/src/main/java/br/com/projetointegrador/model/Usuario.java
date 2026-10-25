package br.com.projetointegrador.model;

// Uso interno do backend: contem o hash, nunca deve ser enviado como resposta da API.
public class Usuario {
    private final long id;
    private final String nome;
    private final String login;
    private final String senhaHash;
    private final String perfil;
    private final boolean ativo;

    public Usuario(long id, String nome, String login, String senhaHash, String perfil, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getLogin() { return login; }
    public String getSenhaHash() { return senhaHash; }
    public String getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
}
