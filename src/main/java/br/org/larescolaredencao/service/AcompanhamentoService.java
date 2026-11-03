package br.org.larescolaredencao.service;

import br.org.larescolaredencao.dto.*;
import br.org.larescolaredencao.model.*;
import br.org.larescolaredencao.model.enums.Perfil;
import br.org.larescolaredencao.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AcompanhamentoService {
    private final UsuarioService usuarios;
    private final OcorrenciaRepository ocorrencias;
    private final EntrevistaSocialRepository entrevistas;
    private final MembroRepository membros;
    private final Clock relogio;

    @org.springframework.beans.factory.annotation.Autowired
    public AcompanhamentoService(UsuarioService usuarios, OcorrenciaRepository ocorrencias,
            EntrevistaSocialRepository entrevistas, MembroRepository membros) {
        this(usuarios, ocorrencias, entrevistas, membros, Clock.system(ZoneId.of("America/Sao_Paulo")));
    }

    AcompanhamentoService(UsuarioService usuarios, OcorrenciaRepository ocorrencias,
            EntrevistaSocialRepository entrevistas, MembroRepository membros, Clock relogio) {
        this.usuarios = usuarios; this.ocorrencias = ocorrencias;
        this.entrevistas = entrevistas; this.membros = membros;
        this.relogio = relogio;
    }

    public Page<OcorrenciaAcompanhamentoDTO> listarOcorrencias(Integer usuario, String search,
            Pageable pageable, Membro membro) {
        usuarios.buscarUsuarioPorId(usuario, membro);
        boolean admin = perfil(membro) == Perfil.ADMINISTRADOR;
        List<Integer> unidades = admin ? List.of(-1) : membro.getUnidades().stream().map(Unidade::getId).toList();
        var pagina = pagina(pageable, "dataOcorrencia");
        if (unidades.isEmpty()) return Page.empty(pagina);
        return ocorrencias.listarAcompanhamento(usuario, admin, unidades, busca(search), pagina)
                .map(OcorrenciaAcompanhamentoDTO::de);
    }

    public Page<EntrevistaResponseDTO> listarEntrevistas(Integer usuario, String search,
            Pageable pageable, Membro membro) {
        validarEntrevistas(usuario, membro);
        var resultado = entrevistas.listarAcompanhamento(usuario, busca(search), pagina(pageable, "dataEntrevista"));
        var autores = membros.findAllById(resultado.stream().map(EntrevistaSocial::getIdMembro).distinct().toList())
                .stream().collect(Collectors.toMap(Membro::getId, Membro::getNomeCompleto));
        return resultado.map(e -> resposta(e, autores.getOrDefault(e.getIdMembro(), "-")));
    }

    @Transactional
    public EntrevistaResponseDTO salvarEntrevista(Integer usuario, Integer id, EntrevistaDTO dto, Membro membro) {
        validarEntrevistas(usuario, membro);
        if (dto.dataEntrevista() == null || dto.relato() == null || dto.relato().isBlank()
                || dto.relato().length() > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe data, horário e descrição válidos.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (dto.dataEntrevista().isAfter(agora)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data e o horário da entrevista não podem estar no futuro.");
        }
        var entrevista = id == null ? new EntrevistaSocial() : buscarEntrevista(usuario, id);
        if (id == null) {
            entrevista.setIdUsuario(usuario);
            entrevista.setIdMembro(membro.getId());
            entrevista.setDataRegistro(agora.withNano(0));
        }
        entrevista.setDataEntrevista(dto.dataEntrevista());
        entrevista.setRelato(dto.relato().trim());
        var salva = entrevistas.save(entrevista);
        String autor = membros.findById(salva.getIdMembro()).map(Membro::getNomeCompleto).orElse("-");
        return resposta(salva, autor);
    }

    @Transactional
    public void excluirEntrevista(Integer usuario, Integer id, Membro membro) {
        validarEntrevistas(usuario, membro);
        entrevistas.delete(buscarEntrevista(usuario, id));
    }

    private EntrevistaSocial buscarEntrevista(Integer usuario, Integer id) {
        return entrevistas.findById(id).filter(e -> usuario.equals(e.getIdUsuario()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrevista não encontrada."));
    }

    private void validarEntrevistas(Integer usuario, Membro membro) {
        if (perfil(membro) != Perfil.ADMINISTRADOR && perfil(membro) != Perfil.COORDENADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem acesso às entrevistas sociais.");
        }
        usuarios.buscarUsuarioPorId(usuario, membro);
    }

    private Perfil perfil(Membro membro) { return Perfil.fromNomePapel(membro.getPapel().getNomePapel()); }

    private Pageable pagina(Pageable pageable, String data) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50),
                Sort.by(Sort.Order.desc(data), Sort.Order.desc("id")));
    }

    private String busca(String search) {
        if (search != null && search.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Busca deve ter até 200 caracteres.");
        }
        return TermoBusca.like(search);
    }

    private EntrevistaResponseDTO resposta(EntrevistaSocial e, String autor) {
        return new EntrevistaResponseDTO(e.getId(), e.getIdUsuario(), e.getIdMembro(), autor,
                e.getDataEntrevista(), e.getRelato(), e.getDataRegistro());
    }
}
