package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarRedeSocialDTO;
import br.org.larescolaredencao.dto.CriarRedeSocialDTO;
import br.org.larescolaredencao.model.RedeSocial;
import br.org.larescolaredencao.repository.RedeSocialRepository;

@Service
public class RedeSocialService {

	private static final String PASTA_ICONES = "redes-sociais/";

	private RedeSocialRepository redeSocialRepository;
	private ArquivoService arquivoService;

	public RedeSocialService(RedeSocialRepository redeSocialRepository, ArquivoService arquivoService) {
		this.redeSocialRepository = redeSocialRepository;
		this.arquivoService = arquivoService;
	}

	public List<RedeSocial> getAllRedesSociais() {
		return redeSocialRepository.findAll();
	}

	public RedeSocial getRedeSocialById(Long id) {
		return redeSocialRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rede social não encontrada."));
	}

	public RedeSocial criarRedeSocial(CriarRedeSocialDTO criarRedeSocialDTO) {
		RedeSocial r = new RedeSocial();
		r.setNome(criarRedeSocialDTO.getNome());
		r.setUrl(criarRedeSocialDTO.getUrl());
		r.setIcone(arquivoService.salvarArquivo(criarRedeSocialDTO.getIcone(), PASTA_ICONES));
		r.setAtivo(true);
		return redeSocialRepository.save(r);
	}

	public RedeSocial atualizarRedeSocial(Long id, AtualizarRedeSocialDTO atualizarRedeSocialDTO) {
		RedeSocial r = getRedeSocialById(id);

		if (atualizarRedeSocialDTO.getNome() != null && !atualizarRedeSocialDTO.getNome().isBlank()) {
			r.setNome(atualizarRedeSocialDTO.getNome());
		}

		if (atualizarRedeSocialDTO.getUrl() != null && !atualizarRedeSocialDTO.getUrl().isBlank()) {
			r.setUrl(atualizarRedeSocialDTO.getUrl());
		}

		if (atualizarRedeSocialDTO.getIcone() != null && !atualizarRedeSocialDTO.getIcone().isEmpty()) {
			arquivoService.deletarArquivo(r.getIcone());
			r.setIcone(arquivoService.salvarArquivo(atualizarRedeSocialDTO.getIcone(), PASTA_ICONES));
		}

		if (atualizarRedeSocialDTO.getAtivo() != null) {
			r.setAtivo(atualizarRedeSocialDTO.getAtivo());
		}

		return redeSocialRepository.save(r);
	}

	public void deletarRedeSocial(Long id) {
		RedeSocial r = getRedeSocialById(id);
		arquivoService.deletarArquivo(r.getIcone());
		redeSocialRepository.delete(r);
	}
}
