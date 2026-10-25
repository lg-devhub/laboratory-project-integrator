package br.com.projetointegrador.model;

import java.util.Base64;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public record ItemDados(String nome, String categoria, String formaRegistro, Boolean equipamento,
                        Integer quantidade, String status, String numeroPatrimonio, Long setorResponsavelId,
                        String descricao, String localizacao, String fotoBase64, String fotoTipo) {
    private static String texto(String valor, int limite, boolean obrigatorio, String campo) {
        String limpo = valor == null ? "" : valor.strip();
        if ((obrigatorio && limpo.isEmpty()) || limpo.codePointCount(0, limpo.length()) > limite)
            throw new IllegalArgumentException(campo + (obrigatorio ? " e obrigatorio e" : "") + " deve ter ate " + limite + " caracteres.");
        return limpo.isEmpty() ? null : limpo;
    }

    public ItemDados validar() {
        if (!List.of("INDIVIDUAL", "QUANTIDADE").contains(formaRegistro == null ? "" : formaRegistro))
            throw new IllegalArgumentException("Selecione a forma de registro.");
        if (equipamento == null) throw new IllegalArgumentException("Informe se o item e um equipamento.");
        if (quantidade == null || quantidade <= 0 || ("INDIVIDUAL".equals(formaRegistro) && quantidade != 1))
            throw new IllegalArgumentException("Informe quantidade inteira positiva; registros individuais devem ter quantidade 1.");
        if (equipamento && !"INDIVIDUAL".equals(formaRegistro))
            throw new IllegalArgumentException("Equipamentos devem ser registrados individualmente.");
        if (!List.of("ATIVO", "EM_MANUTENCAO", "INATIVO").contains(status == null ? "" : status))
            throw new IllegalArgumentException("Selecione um status valido.");
        String patrimonio = texto(numeroPatrimonio, 120, equipamento, "Patrimonio");
        if (equipamento && (setorResponsavelId == null || setorResponsavelId <= 0))
            throw new IllegalArgumentException("Selecione o setor responsavel pelo equipamento.");
        if (!equipamento && (patrimonio != null || setorResponsavelId != null))
            throw new IllegalArgumentException("Patrimonio e setor responsavel se aplicam somente a equipamentos individuais.");
        foto(); // Valida tamanho e formato antes de abrir a transacao.
        return new ItemDados(texto(nome,120,true,"Nome"), texto(categoria,120,true,"Categoria"),
                formaRegistro, equipamento, quantidade, status, patrimonio, setorResponsavelId,
                texto(descricao,2000,false,"Descricao"), texto(localizacao,300,false,"Localizacao"), fotoBase64, fotoTipo);
    }

    public byte[] foto() {
        if (fotoBase64 == null && fotoTipo == null) return null;
        if (fotoBase64 == null || fotoBase64.length() > 2796204
                || !List.of("image/png", "image/jpeg").contains(fotoTipo == null ? "" : fotoTipo))
            throw new IllegalArgumentException("Use uma foto PNG ou JPEG de ate 2 MB.");
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(fotoBase64); }
        catch (IllegalArgumentException erro) { throw new IllegalArgumentException("Foto invalida."); }
        boolean png = bytes.length >= 8 && bytes[0] == (byte)137 && bytes[1] == 80 && bytes[2] == 78
                && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
        boolean jpeg = bytes.length >= 4 && bytes[0] == (byte)255 && bytes[1] == (byte)216
                && bytes[bytes.length-2] == (byte)255 && bytes[bytes.length-1] == (byte)217;
        if (bytes.length > 2097152 || !("image/png".equals(fotoTipo) ? png : jpeg))
            throw new IllegalArgumentException("Use uma foto PNG ou JPEG valida de ate 2 MB.");
        try (var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores=ImageIO.getImageReaders(input);
            if (!leitores.hasNext()) throw new IllegalArgumentException("A foto nao pode ser lida.");
            var leitor=leitores.next();
            try {
                leitor.setInput(input);
                long pixels=(long)leitor.getWidth(0)*leitor.getHeight(0);
                if (pixels>10000000) throw new IllegalArgumentException("Use uma foto de ate 10 megapixels.");
                leitor.read(0); // Rejeita arquivos truncados ou que so imitam o cabecalho de uma imagem.
            } finally { leitor.dispose(); }
        } catch (IOException erro) { throw new IllegalArgumentException("A foto nao pode ser lida."); }
        return bytes;
    }
}
