package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoDetalhadoResponseDTO;
import br.org.larescolaredencao.dto.EventoRedeSocialResponseDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.dto.VincularRedeSocialEventoDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.EventoRedeSocial;
import br.org.larescolaredencao.model.EventoRedeSocialId;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.model.RedeSocial;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.repository.EventoRedeSocialRepository;
import br.org.larescolaredencao.repository.EventoRepository;
import br.org.larescolaredencao.repository.ParceiroRepository;
import br.org.larescolaredencao.repository.RedeSocialRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ArquivoService arquivoService;
    private final ParceiroRepository parceiroRepository;
    private final RedeSocialRepository redeSocialRepository;
    private final EventoRedeSocialRepository eventoRedeSocialRepository;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public EventoService(EventoRepository eventoRepository, ArquivoService arquivoService,
            ParceiroRepository parceiroRepository, RedeSocialRepository redeSocialRepository,
            EventoRedeSocialRepository eventoRedeSocialRepository) {
        this.eventoRepository = eventoRepository;
        this.arquivoService = arquivoService;
        this.parceiroRepository = parceiroRepository;
        this.redeSocialRepository = redeSocialRepository;
        this.eventoRedeSocialRepository = eventoRedeSocialRepository;
    }

    public Page<EventoResponseDTO> getAllEventos(Pageable pageable, TipoEvento tipo) {
        Page<Evento> eventos = tipo != null
                ? eventoRepository.findByTipoEvento(tipo, pageable)
                : eventoRepository.findAll(pageable);
        return eventos.map(EventoResponseDTO::new);
    }

    // Os parceiros NÃO são filtrados por ativo, de propósito: ver EventoRepository.findByIdComParceiros.
    @Transactional(readOnly = true)
    public EventoDetalhadoResponseDTO getEventoById(Integer id) {
        Evento evento = eventoRepository.findByIdComParceiros(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        return new EventoDetalhadoResponseDTO(evento);
    }

    public EventoResponseDTO criarEvento(CriarEventoDTO dto) {
    	if (eventoRepository.existsByTituloAndDataEvento(dto.getTitulo(), dto.getDataEvento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Um evento com este título e data já existe.");
        }
    	
        Evento evento = new Evento();
        evento.setTitulo(dto.getTitulo());
        evento.setDescricao(dto.getDescricao());
        evento.setDataEvento(dto.getDataEvento());
        evento.setEndereco(dto.getEndereco());
        evento.setTipoEvento(dto.getTipoEvento());

        if (dto.getValor() != null) {
            evento.setValor(dto.getValor());
        }

        if (dto.getParceirosIds() != null && !dto.getParceirosIds().isEmpty()) {
            List<Parceiro> parceiros = parceiroRepository.findAllById(dto.getParceirosIds());
            evento.setParceiros(parceiros);
        }
        
        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            String caminhoImagem = arquivoService.salvarArquivo(dto.getImagem(), "eventos/");
            evento.setImagem(caminhoImagem);
        }
        
        
        Evento salvo = eventoRepository.save(evento);
        return new EventoResponseDTO(salvo);
    }

    public EventoResponseDTO atualizarEvento(Integer id, AtualizarEventoDTO dto) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));

        evento.setTitulo(dto.getTitulo());
        evento.setDescricao(dto.getDescricao());
        evento.setDataEvento(dto.getDataEvento());
        evento.setEndereco(dto.getEndereco());
        evento.setValor(dto.getValor());
        evento.setTipoEvento(dto.getTipoEvento());

        if (dto.getParceirosIds() != null && !dto.getParceirosIds().isEmpty()) {
            List<Parceiro> parceiros = parceiroRepository.findAllById(dto.getParceirosIds());
            evento.setParceiros(parceiros);
        } else {
            evento.getParceiros().clear();
        }

        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            String caminhoImagemAntiga = evento.getImagem();
            String novoCaminho = arquivoService.salvarArquivo(dto.getImagem(), "eventos/");
            evento.setImagem(novoCaminho);
            
            if (caminhoImagemAntiga != null) {
                arquivoService.deletarArquivo(caminhoImagemAntiga);
            }
        }

        Evento salvo = eventoRepository.save(evento);
        return new EventoResponseDTO(salvo);
    }

    public void deletarEvento(Integer id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        
        eventoRepository.delete(evento);

        if (evento.getImagem() != null) {
            arquivoService.deletarArquivo(evento.getImagem());
        }
    }

    public List<EventoRedeSocialResponseDTO> listarRedesSociais(Integer idEvento) {
        if (!eventoRepository.existsById(idEvento)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado.");
        }
        return eventoRedeSocialRepository.findByEventoIdOrderByRedeSocialNomeAsc(idEvento).stream()
                .map(EventoRedeSocialResponseDTO::new)
                .toList();
    }

    // Só aceita rede social ativa. Sem link informado, usa a url cadastrada na própria rede social.
    // Se o evento já estiver vinculado a essa rede social, apenas atualiza o link.
    @Transactional
    public EventoRedeSocialResponseDTO vincularRedeSocial(Integer idEvento, VincularRedeSocialEventoDTO dto) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        RedeSocial redeSocial = redeSocialRepository.findById(dto.getIdRedeSocial())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rede social não encontrada."));
        if (!Boolean.TRUE.equals(redeSocial.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rede social inativa não pode ser vinculada.");
        }

        EventoRedeSocialId id = new EventoRedeSocialId(idEvento, dto.getIdRedeSocial());
        EventoRedeSocial vinculo = eventoRedeSocialRepository.findById(id).orElseGet(() -> {
            EventoRedeSocial novo = new EventoRedeSocial();
            novo.setId(id);
            novo.setEvento(evento);
            novo.setRedeSocial(redeSocial);
            return novo;
        });
        boolean linkInformado = dto.getUrlLink() != null && !dto.getUrlLink().isBlank();
        vinculo.setUrlLink(linkInformado ? dto.getUrlLink() : redeSocial.getUrl());

        return new EventoRedeSocialResponseDTO(eventoRedeSocialRepository.save(vinculo));
    }

    public void desvincularRedeSocial(Integer idEvento, Long idRedeSocial) {
        EventoRedeSocialId id = new EventoRedeSocialId(idEvento, idRedeSocial);
        if (!eventoRedeSocialRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rede social não vinculada a este evento.");
        }
        eventoRedeSocialRepository.deleteById(id);
    }
}
