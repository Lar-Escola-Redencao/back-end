package br.org.larescolaredencao.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoDetalhadoResponseDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.dto.PosEventoDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.model.MidiaEvento;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.model.enums.TipoEvento;
import br.org.larescolaredencao.model.enums.TipoMidia;
import br.org.larescolaredencao.repository.EventoRepository;
import br.org.larescolaredencao.repository.MidiaEventoRepository;
import br.org.larescolaredencao.repository.ParceiroRepository;

@Service
public class EventoService {

    private static final int MAX_MIDIAS_POR_EVENTO = 10;

    private final EventoRepository eventoRepository;
    private final ArquivoService arquivoService;
    private final ParceiroRepository parceiroRepository;
    private final MidiaEventoRepository midiaEventoRepository;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public EventoService(EventoRepository eventoRepository, ArquivoService arquivoService,
            ParceiroRepository parceiroRepository, MidiaEventoRepository midiaEventoRepository) {
        this.eventoRepository = eventoRepository;
        this.arquivoService = arquivoService;
        this.parceiroRepository = parceiroRepository;
        this.midiaEventoRepository = midiaEventoRepository;
    }

    public Page<EventoResponseDTO> getAllEventos(Pageable pageable, TipoEvento tipo) {
        Page<Evento> eventos = tipo != null
                ? eventoRepository.findByTipoEvento(tipo, pageable)
                : eventoRepository.findAll(pageable);
        return eventos.map(EventoResponseDTO::new);
    }

    // Duas consultas fixas (evento+parceiros via JOIN FETCH, e mídias), independente da quantidade
    // de itens. Não dá para buscar as duas listas num único JOIN FETCH (MultipleBagFetchException).
    // Os parceiros NÃO são filtrados por ativo, de propósito: ver EventoRepository.findByIdComParceiros.
    @Transactional(readOnly = true)
    public EventoDetalhadoResponseDTO getEventoById(Integer id) {
        Evento evento = eventoRepository.findByIdComParceiros(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        List<MidiaEvento> midias = midiaEventoRepository.findByEventoIdOrderByIdAsc(id);
        return new EventoDetalhadoResponseDTO(evento, midias);
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

    @Transactional
    public EventoDetalhadoResponseDTO atualizarPosEvento(Integer id, PosEventoDTO dto) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));

        if (!evento.getDataEvento().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O evento ainda não foi concluído.");
        }

        long midiasExistentes = midiaEventoRepository.countByEventoId(id);
        long novasMidias = contarNovasMidias(dto);
        if (midiasExistentes + novasMidias > MAX_MIDIAS_POR_EVENTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Limite máximo de " + MAX_MIDIAS_POR_EVENTO + " mídias por evento excedido.");
        }

        List<MidiaEvento> novasEntidades = construirNovasMidias(dto, evento);
        if (!novasEntidades.isEmpty()) {
            midiaEventoRepository.saveAll(novasEntidades);
        }

        if (dto.getComentarioPosEvento() != null) {
            evento.setComentarioPosEvento(dto.getComentarioPosEvento());
            eventoRepository.save(evento);
        }

        List<MidiaEvento> midias = midiaEventoRepository.findByEventoIdOrderByIdAsc(id);
        return new EventoDetalhadoResponseDTO(evento, midias);
    }

    private long contarNovasMidias(PosEventoDTO dto) {
        return contarArquivosValidos(dto.getImagens()) + contarArquivosValidos(dto.getVideos())
                + contarUrlsValidas(dto.getImagensUrls()) + contarUrlsValidas(dto.getVideosUrls());
    }

    private long contarArquivosValidos(List<MultipartFile> arquivos) {
        if (arquivos == null) {
            return 0;
        }
        return arquivos.stream().filter(arquivo -> arquivo != null && !arquivo.isEmpty()).count();
    }

    private long contarUrlsValidas(List<String> urls) {
        if (urls == null) {
            return 0;
        }
        return urls.stream().filter(url -> url != null && !url.isBlank()).count();
    }

    private List<MidiaEvento> construirNovasMidias(PosEventoDTO dto, Evento evento) {
        List<MidiaEvento> novasMidias = new ArrayList<>();

        if (dto.getImagens() != null) {
            for (MultipartFile arquivo : dto.getImagens()) {
                if (arquivo != null && !arquivo.isEmpty()) {
                    String url = arquivoService.salvarArquivo(arquivo, "eventos/pos-evento/", TipoArquivo.FOTO);
                    novasMidias.add(criarMidia(url, TipoMidia.IMAGEM, evento));
                }
            }
        }

        if (dto.getVideos() != null) {
            for (MultipartFile arquivo : dto.getVideos()) {
                if (arquivo != null && !arquivo.isEmpty()) {
                    String url = arquivoService.salvarArquivo(arquivo, "eventos/pos-evento/", TipoArquivo.VIDEO);
                    novasMidias.add(criarMidia(url, TipoMidia.VIDEO, evento));
                }
            }
        }

        if (dto.getImagensUrls() != null) {
            for (String url : dto.getImagensUrls()) {
                if (url != null && !url.isBlank()) {
                    novasMidias.add(criarMidia(url, TipoMidia.IMAGEM, evento));
                }
            }
        }

        if (dto.getVideosUrls() != null) {
            for (String url : dto.getVideosUrls()) {
                if (url != null && !url.isBlank()) {
                    novasMidias.add(criarMidia(url, TipoMidia.VIDEO, evento));
                }
            }
        }

        return novasMidias;
    }

    private MidiaEvento criarMidia(String url, TipoMidia tipo, Evento evento) {
        MidiaEvento midia = new MidiaEvento();
        midia.setUrlMidia(url);
        midia.setTipoMidia(tipo);
        midia.setEvento(evento);
        return midia;
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