package br.org.larescolaredencao.dto;

public class AtualizarUsuarioDTO extends CadastroUsuarioCompletoDTO {
    private boolean removerFotoPerfil;

    public boolean isRemoverFotoPerfil() {
        return removerFotoPerfil;
    }

    public void setRemoverFotoPerfil(boolean removerFotoPerfil) {
        this.removerFotoPerfil = removerFotoPerfil;
    }
}
