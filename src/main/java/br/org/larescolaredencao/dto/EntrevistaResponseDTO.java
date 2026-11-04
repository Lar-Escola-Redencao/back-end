package br.org.larescolaredencao.dto;

import java.time.LocalDateTime;

public record EntrevistaResponseDTO(Integer id, Integer idUsuario, Integer idMembro,
        String nomeResponsavel, LocalDateTime dataEntrevista, String relato, LocalDateTime dataRegistro) { }
