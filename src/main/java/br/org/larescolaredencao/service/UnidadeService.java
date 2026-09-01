package br.org.larescolaredencao.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarUnidadeDTO;
import br.org.larescolaredencao.dto.CriarUnidadeDTO;
import br.org.larescolaredencao.model.Unidade;
import br.org.larescolaredencao.repository.UnidadeRepository;

@Service
public class UnidadeService {

    private static final String COR_HEX_PADRAO = "#F5F5F5";
    private static final String PASTA_IMAGENS = "unidades/";

    private final UnidadeRepository unidadeRepository;
    private final ArquivoService arquivoService;

    public UnidadeService(UnidadeRepository unidadeRepository, ArquivoService arquivoService) {
        this.unidadeRepository = unidadeRepository;
        this.arquivoService = arquivoService;
    }

    public List<Unidade> getAllUnidades() {
        return unidadeRepository.findAll();
    }

    public Unidade getUnidadeById(Integer id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidade não encontrada."));
    }

    public Unidade criarUnidade(CriarUnidadeDTO dto) {
        validarFaixaEtaria(dto.getIdadeMin(), dto.getIdadeMax());
        validarHorarioFuncionamento(dto.getHorarioAbertura(), dto.getHorarioFechamento());

        if (unidadeRepository.findByNome(dto.getNome()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma unidade com este nome.");
        }

        Unidade unidade = new Unidade();
        unidade.setNome(dto.getNome());
        unidade.setEndereco(dto.getEndereco());
        unidade.setTelefone(dto.getTelefone());
        unidade.setEmail(dto.getEmail());
        unidade.setDiasFuncionamento(dto.getDiasFuncionamento());
        unidade.setHorarioAbertura(dto.getHorarioAbertura());
        unidade.setHorarioFechamento(dto.getHorarioFechamento());
        unidade.setIdadeMin(dto.getIdadeMin());
        unidade.setIdadeMax(dto.getIdadeMax());
        unidade.setCorHex(dto.getCorHex() != null && !dto.getCorHex().isBlank() ? dto.getCorHex() : COR_HEX_PADRAO);

        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            unidade.setImagem(arquivoService.salvarArquivo(dto.getImagem(), PASTA_IMAGENS));
        }

        return unidadeRepository.save(unidade);
    }

    public Unidade atualizarUnidade(Integer id, AtualizarUnidadeDTO dto) {
        Unidade unidade = getUnidadeById(id);

        validarFaixaEtaria(dto.getIdadeMin(), dto.getIdadeMax());
        validarHorarioFuncionamento(dto.getHorarioAbertura(), dto.getHorarioFechamento());

        if (!unidade.getNome().equals(dto.getNome())) {
            Optional<Unidade> unidadeComNome = unidadeRepository.findByNome(dto.getNome());
            if (unidadeComNome.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma unidade com este nome.");
            }
        }

        unidade.setNome(dto.getNome());
        unidade.setEndereco(dto.getEndereco());
        unidade.setTelefone(dto.getTelefone());
        unidade.setEmail(dto.getEmail());
        unidade.setDiasFuncionamento(dto.getDiasFuncionamento());
        unidade.setHorarioAbertura(dto.getHorarioAbertura());
        unidade.setHorarioFechamento(dto.getHorarioFechamento());
        unidade.setIdadeMin(dto.getIdadeMin());
        unidade.setIdadeMax(dto.getIdadeMax());
        unidade.setCorHex(dto.getCorHex() != null && !dto.getCorHex().isBlank() ? dto.getCorHex() : COR_HEX_PADRAO);

        if (dto.getImagem() != null && !dto.getImagem().isEmpty()) {
            String imagemAntiga = unidade.getImagem();
            unidade.setImagem(arquivoService.salvarArquivo(dto.getImagem(), PASTA_IMAGENS));

            if (imagemAntiga != null) {
                arquivoService.deletarArquivo(imagemAntiga);
            }
        }

        return unidadeRepository.save(unidade);
    }

    public void deletarUnidade(Integer id) {
        Unidade unidade = getUnidadeById(id);
        unidadeRepository.delete(unidade);

        if (unidade.getImagem() != null) {
            arquivoService.deletarArquivo(unidade.getImagem());
        }
    }

    private void validarFaixaEtaria(Integer idadeMin, Integer idadeMax) {
        if (idadeMin != null && idadeMax != null && idadeMin > idadeMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A idade mínima não pode ser maior que a idade máxima.");
        }
    }

    private void validarHorarioFuncionamento(LocalTime horarioAbertura, LocalTime horarioFechamento) {
        if (horarioAbertura != null && horarioFechamento != null && !horarioAbertura.isBefore(horarioFechamento)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário de abertura deve ser anterior ao horário de fechamento.");
        }
    }
}
