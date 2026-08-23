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

    public List<EventoResponseDTO> getAllEventos() {
        return eventoRepository.findAll()
                .stream()
                .map(EventoResponseDTO::new)
                .collect(Collectors.toList());
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
        
        String caminhoImagem = arquivoService.salvarArquivo(dto.getImagem(), "eventos/");
        evento.setImagem(caminhoImagem);

        Evento salvo = eventoRepository.save(evento);
        return new EventoResponseDTO(salvo);
    }

    public EventoResponseDTO atualizarEvento(Integer id, AtualizarEventoDTO dto) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));

        boolean alterouAlgo = false;

        if (dto.getTitulo() != null && !dto.getTitulo().trim().isEmpty() && !isTextoIgual(evento.getTitulo(), dto.getTitulo())) {
            evento.setTitulo(dto.getTitulo());
            alterouAlgo = true;
        }
        
        if (dto.getDescricao() != null && !dto.getDescricao().trim().isEmpty() && !isTextoIgual(evento.getDescricao(), dto.getDescricao())) {
            evento.setDescricao(dto.getDescricao());
            alterouAlgo = true;
        }
        
        if (dto.getDataEvento() != null && (evento.getDataEvento() == null || !evento.getDataEvento().truncatedTo(ChronoUnit.SECONDS).equals(dto.getDataEvento().truncatedTo(ChronoUnit.SECONDS)))) {
            evento.setDataEvento(dto.getDataEvento());
            alterouAlgo = true;
        }
        
        if (dto.getEndereco() != null && !dto.getEndereco().trim().isEmpty() && !isTextoIgual(evento.getEndereco(), dto.getEndereco())) {
            evento.setEndereco(dto.getEndereco());
            alterouAlgo = true;
        }
        
        if (dto.getValor() != null && (evento.getValor() == null || evento.getValor().compareTo(dto.getValor()) != 0)) {
            evento.setValor(dto.getValor());
            alterouAlgo = true;
        }
        
        if (dto.getTipoEvento() != null && evento.getTipoEvento() != dto.getTipoEvento()) {
            evento.setTipoEvento(dto.getTipoEvento());
            alterouAlgo = true;
        }
        
        if (dto.getComentarioPosEvento() != null && !isTextoIgual(evento.getComentarioPosEvento(), dto.getComentarioPosEvento())) {
            evento.setComentarioPosEvento(dto.getComentarioPosEvento());
            alterouAlgo = true;
        }

        if (dto.getParceirosIds() != null) {
            if (!isListasParceirosIguais(evento.getParceiros(), dto.getParceirosIds())) {
                if (!dto.getParceirosIds().isEmpty()) {
                    List<Parceiro> parceiros = parceiroRepository.findAllById(dto.getParceirosIds());
                    evento.setParceiros(parceiros);
                } else {
                    evento.getParceiros().clear();
                }
                alterouAlgo = true;
            }
        }

        boolean imagemFoiEnviada = dto.getImagem() != null && !dto.getImagem().isEmpty();
        if (imagemFoiEnviada) {
            alterouAlgo = true;
            String caminhoImagemAntiga = evento.getImagem();
            String novoCaminho = arquivoService.salvarArquivo(dto.getImagem(), "eventos/");
            evento.setImagem(novoCaminho);
            
            if (caminhoImagemAntiga != null) {
                arquivoService.deletarArquivo(caminhoImagemAntiga);
            }
        }

        if (!alterouAlgo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum dado foi alterado. A atualização não foi realizada.");
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
    
    private boolean isTextoIgual(String str1, String str2) {
        String s1 = (str1 == null) ? "" : str1.trim();
        String s2 = (str2 == null) ? "" : str2.trim();
        return s1.equals(s2);
    }
    
    private boolean isListasParceirosIguais(List<Parceiro> parceirosAtuais, List<Long> parceirosIdsNovos) {
        boolean atuaisVazio = (parceirosAtuais == null || parceirosAtuais.isEmpty());
        boolean novosVazio = (parceirosIdsNovos == null || parceirosIdsNovos.isEmpty());
        
        if (atuaisVazio && novosVazio) return true;
        if (atuaisVazio || novosVazio) return false;
        if (parceirosAtuais.size() != parceirosIdsNovos.size()) return false;
        
        List<Long> idsAtuais = parceirosAtuais.stream().map(Parceiro::getId).collect(Collectors.toList());
        return idsAtuais.containsAll(parceirosIdsNovos) && parceirosIdsNovos.containsAll(idsAtuais);
    }
}