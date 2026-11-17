package br.org.larescolaredencao.dto;

public record RecuperacaoSenhaRespostaDTO(long retryAfterSeconds, int tentativas) {}
