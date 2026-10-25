package br.com.projetointegrador.model;

import java.util.List;

// Somente dados editaveis: a rota e o cadastro atual determinam identidade e vinculos.
public record ItemEdicao(String nome, String categoria, Integer quantidade, String status,
                         String descricao, String localizacao, Long versao,
                         String fotoAcao, String fotoBase64, String fotoTipo) {
    public String acaoFoto() { return fotoAcao == null ? "MANTER" : fotoAcao; }

    public ItemDados validar(Item atual) {
        if (!List.of("MANTER","SUBSTITUIR","REMOVER").contains(acaoFoto()))
            throw new IllegalArgumentException("Selecione uma acao valida para a foto.");
        if ("SUBSTITUIR".equals(acaoFoto())) {
            if (fotoBase64 == null || fotoTipo == null) throw new IllegalArgumentException("Informe a nova foto.");
        } else if (fotoBase64 != null || fotoTipo != null) {
            throw new IllegalArgumentException("Envie uma foto apenas ao substitui-la.");
        }
        return new ItemDados(nome,categoria,atual.formaRegistro(),atual.equipamento(),quantidade,status,
                atual.numeroPatrimonio(),atual.setorResponsavelId(),descricao,localizacao,fotoBase64,fotoTipo).validar();
    }
}
