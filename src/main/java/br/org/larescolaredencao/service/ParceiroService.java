package br.org.larescolaredencao.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarParceiroDTO;
import br.org.larescolaredencao.dto.CriarParceiroDTO;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.repository.ParceiroRepository;

@Service
public class ParceiroService {

	private static final String PASTA_LOGOS = "parceiros/";

	private ParceiroRepository parceiroRepository;
	private ArquivoService arquivoService;

	public ParceiroService(ParceiroRepository parceiroRepository, ArquivoService arquivoService) {
		this.parceiroRepository = parceiroRepository;
		this.arquivoService = arquivoService;
	}

	public Page<Parceiro> getAllParceiros(Pageable pageable) {
		return getAllParceiros(pageable, null);
	}

	public Page<Parceiro> getAllParceiros(Pageable pageable, String search) {
		String termo = TermoBusca.like(search);
		if (termo != null) {
			return parceiroRepository.buscar(termo, pageable);
		}
		return parceiroRepository.findAll(pageable);
	}

	public Parceiro getParceiroById(Long id) {
		return parceiroRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado."));
	}

	public Parceiro criarParceiro(CriarParceiroDTO criarParceiroDTO) {
		Parceiro p = new Parceiro();
		p.setNome(criarParceiroDTO.getNome());
		p.setLogo(arquivoService.salvarArquivo(criarParceiroDTO.getLogo(), PASTA_LOGOS));
		p.setAtivo(true);
		return parceiroRepository.save(p);
	}

	public Parceiro atualizarParceiro(Long id, AtualizarParceiroDTO atualizarParceiroDTO) {
		Parceiro p = getParceiroById(id);
		String logoAntiga = p.getLogo();
		String novaLogo = null;

		if (atualizarParceiroDTO.getNome() != null && !atualizarParceiroDTO.getNome().isBlank()) {
			p.setNome(atualizarParceiroDTO.getNome());
		}

		if (atualizarParceiroDTO.getLogo() != null && !atualizarParceiroDTO.getLogo().isEmpty()) {
			novaLogo = arquivoService.salvarArquivo(atualizarParceiroDTO.getLogo(), PASTA_LOGOS);
			p.setLogo(novaLogo);
		}

		if (atualizarParceiroDTO.getAtivo() != null) {
			p.setAtivo(atualizarParceiroDTO.getAtivo());
		}

		Parceiro salvo;
		try {
			salvo = parceiroRepository.save(p);
		} catch (RuntimeException e) {
			if (novaLogo != null) {
				p.setLogo(logoAntiga);
				arquivoService.deletarArquivo(novaLogo);
			}
			throw e;
		}
		if (novaLogo != null) {
			arquivoService.deletarArquivo(logoAntiga);
		}
		return salvo;
	}

	public void deletarParceiro(Long id) {
		Parceiro p = getParceiroById(id);
		arquivoService.deletarArquivo(p.getLogo());
		parceiroRepository.delete(p);
	}
}
