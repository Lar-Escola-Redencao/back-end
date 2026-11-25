package br.org.larescolaredencao.service;

import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import java.time.Clock;

/** Permite a testes de outros pacotes usar o construtor com relógio fixo. */
public class FrequenciaAcompanhamentoServiceTestAcesso extends FrequenciaAcompanhamentoService {
    public FrequenciaAcompanhamentoServiceTestAcesso(UsuarioService usuarios, MatriculaRepository matriculas,
            FrequenciaRepository frequencias, Clock relogio) {
        super(usuarios, matriculas, frequencias, relogio);
    }
}
