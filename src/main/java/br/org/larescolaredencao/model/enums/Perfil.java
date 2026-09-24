package br.org.larescolaredencao.model.enums;

import java.util.Arrays;

/**
 * Perfis de acesso dos membros, mapeados a partir da tabela papel (coluna nome_papel).
 * A ordem das constantes reflete a hierarquia: ADMINISTRADOR > COORDENADOR > MONITOR.
 */
public enum Perfil {
    ADMINISTRADOR(3),
    COORDENADOR(2),
    MONITOR(1);

    private final int nivel;

    Perfil(int nivel) {
        this.nivel = nivel;
    }

    public int getNivel() {
        return nivel;
    }

    public boolean possuiNivelMinimo(Perfil perfil) {
        return this.nivel >= perfil.nivel;
    }

    public static Perfil fromNomePapel(String nomePapel) {
        if (nomePapel == null) {
            throw new IllegalArgumentException("Nome do papel não pode ser nulo");
        }
        return Arrays.stream(values())
                .filter(perfil -> perfil.name().equalsIgnoreCase(nomePapel.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Papel não mapeado para um perfil: " + nomePapel));
    }
}
