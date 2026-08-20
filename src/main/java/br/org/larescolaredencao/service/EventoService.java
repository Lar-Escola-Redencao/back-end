package br.org.larescolaredencao.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.org.larescolaredencao.dto.AtualizarEventoDTO;
import br.org.larescolaredencao.dto.CriarEventoDTO;
import br.org.larescolaredencao.dto.EventoResponseDTO;
import br.org.larescolaredencao.model.Evento;
import br.org.larescolaredencao.repository.EventoRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
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
        Evento evento = new Evento();
        evento.setTitulo(dto.getTitulo());
        evento.setDescricao(dto.getDescricao());
        evento.setDataEvento(dto.getDataEvento());
        evento.setEndereco(dto.getEndereco());
        evento.setImagem(dto.getImagem());
        evento.setTipoEvento(dto.getTipoEvento());

        if (dto.getValor() != null) {
            evento.setValor(dto.getValor());
        }

        Evento salvo = eventoRepository.save(evento);
        return new EventoResponseDTO(salvo);
    }

    public EventoResponseDTO atualizarEvento(Integer id, AtualizarEventoDTO dto) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        boolean nenhumaAlteracao = Objects.equals(evento.getTitulo(), dto.getTitulo()) &&
                Objects.equals(evento.getDescricao(), dto.getDescricao()) &&
                Objects.equals(evento.getDataEvento(), dto.getDataEvento()) &&
                Objects.equals(evento.getEndereco(), dto.getEndereco()) &&
                Objects.equals(evento.getImagem(), dto.getImagem()) &&
                Objects.equals(evento.getValor(), dto.getValor()) &&
                Objects.equals(evento.getTipoEvento(), dto.getTipoEvento()) &&
                Objects.equals(evento.getComentarioPosEvento(), dto.getComentarioPosEvento());

        if (nenhumaAlteracao) {
            throw new RuntimeException("Nenhum dado foi alterado. A atualização não foi realizada.");
        }

        evento.setTitulo(dto.getTitulo());
        evento.setDescricao(dto.getDescricao());
        evento.setDataEvento(dto.getDataEvento());
        evento.setEndereco(dto.getEndereco());
        evento.setImagem(dto.getImagem());
        evento.setValor(dto.getValor() != null ? dto.getValor() : evento.getValor());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setComentarioPosEvento(dto.getComentarioPosEvento());

        Evento salvo = eventoRepository.save(evento);
        return new EventoResponseDTO(salvo);
    }

    public void deletarEvento(Integer id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));
        eventoRepository.delete(evento);
    }
}