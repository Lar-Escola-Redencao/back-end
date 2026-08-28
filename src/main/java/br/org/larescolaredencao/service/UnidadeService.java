package br.org.larescolaredencao.service;

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

    private final UnidadeRepository unidadeRepository;

    public UnidadeService(UnidadeRepository unidadeRepository) {
        this.unidadeRepository = unidadeRepository;
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

        if (unidadeRepository.findByNome(dto.getNome()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma unidade com este nome.");
        }

        Unidade unidade = new Unidade();
        unidade.setNome(dto.getNome());
        unidade.setEndereco(dto.getEndereco());
        unidade.setTelefone(dto.getTelefone());
        unidade.setEmail(dto.getEmail());
        unidade.setDiasFuncionamento(dto.getDiasFuncionamento());
        unidade.setIdadeMin(dto.getIdadeMin());
        unidade.setIdadeMax(dto.getIdadeMax());
        unidade.setCorHex(dto.getCorHex() != null && !dto.getCorHex().isBlank() ? dto.getCorHex() : COR_HEX_PADRAO);

        return unidadeRepository.save(unidade);
    }

    public Unidade atualizarUnidade(Integer id, AtualizarUnidadeDTO dto) {
        Unidade unidade = getUnidadeById(id);

        validarFaixaEtaria(dto.getIdadeMin(), dto.getIdadeMax());

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
        unidade.setIdadeMin(dto.getIdadeMin());
        unidade.setIdadeMax(dto.getIdadeMax());
        unidade.setCorHex(dto.getCorHex() != null && !dto.getCorHex().isBlank() ? dto.getCorHex() : COR_HEX_PADRAO);

        return unidadeRepository.save(unidade);
    }

    public void deletarUnidade(Integer id) {
        Unidade unidade = getUnidadeById(id);
        unidadeRepository.delete(unidade);
    }

    private void validarFaixaEtaria(Integer idadeMin, Integer idadeMax) {
        if (idadeMin != null && idadeMax != null && idadeMin > idadeMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A idade mínima não pode ser maior que a idade máxima.");
        }
    }
}
