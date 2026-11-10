package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.FrequenciaAcompanhamentoDTO;
import br.org.larescolaredencao.dto.MatriculaHistoricoResponseDTO;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.repository.FrequenciaRepository;
import br.org.larescolaredencao.repository.MatriculaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FrequenciaAcompanhamentoService {
    private final UsuarioService usuarios;
    private final MatriculaRepository matriculas;
    private final FrequenciaRepository frequencias;
    private final Clock relogio;

    @Autowired
    public FrequenciaAcompanhamentoService(UsuarioService usuarios, MatriculaRepository matriculas,
            FrequenciaRepository frequencias) {
        this(usuarios, matriculas, frequencias, Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    FrequenciaAcompanhamentoService(UsuarioService usuarios, MatriculaRepository matriculas,
            FrequenciaRepository frequencias, Clock relogio) {
        this.usuarios = usuarios; this.matriculas = matriculas;
        this.frequencias = frequencias; this.relogio = relogio;
    }

    public List<MatriculaHistoricoResponseDTO> matriculas(Integer usuario, Membro membro) {
        return acessiveis(usuario, membro).stream().map(MatriculaHistoricoResponseDTO::new).toList();
    }

    /** Período efetivo de uma consulta de acompanhamento (ambas as datas inclusivas). */
    public record Intervalo(LocalDate inicio, LocalDate fim) {}

    /** Seleção de matrículas acessíveis e período, com as mesmas regras e padrões da consulta de frequências. */
    public record Selecao(Intervalo intervalo, List<Matricula> matriculas) {}

    public Selecao selecionar(Integer usuario, Integer matricula, LocalDate inicio, LocalDate fim, Membro membro) {
        Intervalo intervalo = intervalo(inicio, fim);
        List<Matricula> acessiveis = acessiveis(usuario, membro);
        if (matricula != null) {
            acessiveis = acessiveis.stream().filter(m -> matricula.equals(m.getId())).toList();
            if (acessiveis.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada ou sem acesso.");
            }
        }
        return new Selecao(intervalo, acessiveis);
    }

    private Intervalo intervalo(LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) {
            var hoje = LocalDate.now(relogio);
            return new Intervalo(hoje.withDayOfYear(1), hoje.withMonth(12).withDayOfMonth(31));
        } else if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe as duas datas; a data final não pode ser anterior à inicial.");
        }
        return new Intervalo(inicio, fim);
    }

    public FrequenciaAcompanhamentoDTO consultar(Integer usuario, Integer matricula, LocalDate inicio,
            LocalDate fim, Membro membro) {
        var selecao = selecionar(usuario, matricula, inicio, fim, membro);
        inicio = selecao.intervalo().inicio(); fim = selecao.intervalo().fim();
        List<Integer> ids = selecao.matriculas().stream().map(Matricula::getId).toList();
        List<FrequenciaAcompanhamentoDTO.Mes> meses = ids.isEmpty() ? List.of()
                : frequencias.resumirPorMatriculasEPeriodo(ids, inicio, fim).stream()
                    .map(m -> new FrequenciaAcompanhamentoDTO.Mes(m.getAno(), m.getMes(), m.getPresencas(), m.getFaltas()))
                    .sorted(Comparator.comparingInt(FrequenciaAcompanhamentoDTO.Mes::ano)
                            .thenComparingInt(FrequenciaAcompanhamentoDTO.Mes::mes)).toList();
        long presencas = meses.stream().mapToLong(FrequenciaAcompanhamentoDTO.Mes::presencas).sum();
        long faltas = meses.stream().mapToLong(FrequenciaAcompanhamentoDTO.Mes::faltas).sum();
        return new FrequenciaAcompanhamentoDTO(inicio, fim, meses, presencas, faltas, presencas + faltas);
    }

    private List<Matricula> acessiveis(Integer usuario, Membro membro) {
        usuarios.buscarUsuarioPorId(usuario, membro);
        boolean admin = Perfil.fromNomePapel(membro.getPapel().getNomePapel()) == Perfil.ADMINISTRADOR;
        List<Integer> unidades = admin ? List.of(-1) : membro.getUnidades().stream().map(Unidade::getId).toList();
        return unidades.isEmpty() ? List.of() : matriculas.listarParaAcompanhamento(usuario, admin, unidades);
    }
}
