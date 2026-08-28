package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.repository.EventoRepository;
import br.org.larescolaredencao.repository.ParceiroRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ArquivoService arquivoService;
    private final ParceiroRepository parceiroRepository;
    
    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public EventoService(EventoRepository eventoRepository, ArquivoService arquivoService, ParceiroRepository parceiroRepository) {
        this.eventoRepository = eventoRepository;
        this.arquivoService = arquivoService;
        this.parceiroRepository = parceiroRepository;
    }

    public Page<EventoResponseDTO> getAllEventos(Pageable pageable) {
        return eventoRepository.findAll(pageable)
                .map(EventoResponseDTO::new);
    }

    public EventoResponseDTO getEventoById(Integer id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        return new EventoResponseDTO(evento);
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
        evento.setComentarioPosEvento(dto.getComentarioPosEvento());

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
}