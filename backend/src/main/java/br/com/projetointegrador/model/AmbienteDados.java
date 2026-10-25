package br.com.projetointegrador.model;

import java.util.List;

// Dados editaveis. Perfil, identidade e permissao nunca vem deste formulario.
public record AmbienteDados(String nome, String tipo, String descricao, String cor) {
    public AmbienteDados validar() {
        String nomeLimpo = nome == null ? "" : nome.strip();
        if (nomeLimpo.isEmpty() || nomeLimpo.codePointCount(0, nomeLimpo.length()) > 120)
            throw new IllegalArgumentException("Informe um nome de ate 120 caracteres.");
        if (!List.of("SALA", "SETOR").contains(tipo == null ? "" : tipo))
            throw new IllegalArgumentException("Selecione Sala ou Setor.");
        if (!List.of("TERRACOTA", "AZUL", "VERDE", "VIOLETA", "GRAFITE").contains(cor == null ? "" : cor))
            throw new IllegalArgumentException("Selecione uma das cores disponiveis.");
        // Limite tecnico do formulario, nao uma regra de negocio do ERS.
        if (descricao != null && descricao.codePointCount(0, descricao.length()) > 2000)
            throw new IllegalArgumentException("A descricao deve ter ate 2000 caracteres.");
        return new AmbienteDados(nomeLimpo, tipo,
                descricao == null || descricao.isBlank() ? null : descricao.strip(), cor);
    }
}
