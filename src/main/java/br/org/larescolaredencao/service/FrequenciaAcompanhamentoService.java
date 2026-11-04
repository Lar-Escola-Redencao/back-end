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

    public FrequenciaAcompanhamentoDTO consultar(Integer usuario, Integer matricula, LocalDate inicio,
            LocalDate fim, Membro membro) {
        if (inicio == null && fim == null) {
            var hoje = LocalDate.now(relogio);
            inicio = hoje.withDayOfYear(1); fim = hoje.withMonth(12).withDayOfMonth(31);
        } else if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe as duas datas; a data final não pode ser anterior à inicial.");
        }
        List<Integer> ids = acessiveis(usuario, membro).stream().map(Matricula::getId).toList();
        if (matricula != null) {
            if (!ids.contains(matricula)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada ou sem acesso.");
            }
            ids = List.of(matricula);
        }
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
