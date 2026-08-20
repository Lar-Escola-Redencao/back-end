package br.org.larescolaredencao.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.repository.EventoRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ArquivoService arquivoService;
    
    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public EventoService(EventoRepository eventoRepository, ArquivoService arquivoService) {
        this.eventoRepository = eventoRepository;
        this.arquivoService = arquivoService;
    }

    public List<EventoResponseDTO> getAllEventos() {
        return eventoRepository.findAll()
                .stream()
                .map(EventoResponseDTO::new)
                .collect(Collectors.toList());
    }

    public EventoResponseDTO getEventoById(Integer id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));
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
        
        String caminhoImagem = arquivoService.salvarArquivo(dto.getImagem(), "eventos/");
        evento.setImagem(caminhoImagem);

        Evento salvo = eventoRepository.save(evento);
        return new EventoResponseDTO(salvo);
    }

    public EventoResponseDTO atualizarEvento(Integer id, AtualizarEventoDTO dto) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        boolean valorIgual = (evento.getValor() == null && dto.getValor() == null) ||
                             (evento.getValor() != null && dto.getValor() != null &&
                              evento.getValor().compareTo(dto.getValor()) == 0);

        boolean dataIgual = (evento.getDataEvento() == null && dto.getDataEvento() == null) ||
                            (evento.getDataEvento() != null && dto.getDataEvento() != null &&
                             evento.getDataEvento().truncatedTo(ChronoUnit.SECONDS)
                                   .equals(dto.getDataEvento().truncatedTo(ChronoUnit.SECONDS)));

        boolean imagemFoiEnviada = dto.getImagem() != null && !dto.getImagem().isEmpty();

        boolean nenhumaAlteracao = isTextoIgual(evento.getTitulo(), dto.getTitulo()) &&
                isTextoIgual(evento.getDescricao(), dto.getDescricao()) &&
                dataIgual &&
                isTextoIgual(evento.getEndereco(), dto.getEndereco()) &&
                valorIgual &&
                evento.getTipoEvento() == dto.getTipoEvento() &&
                isTextoIgual(evento.getComentarioPosEvento(), dto.getComentarioPosEvento()) &&
                !imagemFoiEnviada;

        if (nenhumaAlteracao) {
            throw new RuntimeException("Nenhum dado foi alterado. A atualização não foi realizada.");
        }

        String caminhoImagemAntiga = evento.getImagem();

        evento.setTitulo(dto.getTitulo());
        evento.setDescricao(dto.getDescricao());
        evento.setDataEvento(dto.getDataEvento());
        evento.setEndereco(dto.getEndereco());
        
        if (dto.getValor() != null) {
            evento.setValor(dto.getValor());
        }
        
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setComentarioPosEvento(dto.getComentarioPosEvento());

        if (imagemFoiEnviada) {
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
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        if (evento.getImagem() != null) {
            arquivoService.deletarArquivo(evento.getImagem());
        }

        eventoRepository.delete(evento);
    }
    
    private boolean isTextoIgual(String str1, String str2) {
        String s1 = (str1 == null) ? "" : str1.trim();
        String s2 = (str2 == null) ? "" : str2.trim();
        return s1.equals(s2);
    }
}