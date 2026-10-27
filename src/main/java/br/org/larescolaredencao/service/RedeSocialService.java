package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarRedeSocialDTO;
import br.org.larescolaredencao.dto.CriarRedeSocialDTO;
import br.org.larescolaredencao.model.RedeSocial;
import br.org.larescolaredencao.repository.RedeSocialRepository;
import br.org.larescolaredencao.repository.EventoRedeSocialRepository;

@Transactional
@Service
public class RedeSocialService {

	private static final String PASTA_ICONES = "redes-sociais/";

	private RedeSocialRepository redeSocialRepository;
	private ArquivoService arquivoService;
	private final EventoRedeSocialRepository vinculoRepository;

	public RedeSocialService(RedeSocialRepository redeSocialRepository, ArquivoService arquivoService,
			EventoRedeSocialRepository vinculoRepository) {
		this.redeSocialRepository = redeSocialRepository;
		this.arquivoService = arquivoService;
		this.vinculoRepository = vinculoRepository;
	}

	public List<RedeSocial> getAllRedesSociais() {
		return redeSocialRepository.findAll();
	}

	public Page<RedeSocial> getAllRedesSociaisPaginado(Pageable pageable) {
		return redeSocialRepository.findAll(pageable);
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
		vinculoRepository.deleteAll(vinculoRepository.findByRedeSocialId(id));
		vinculoRepository.flush();
		redeSocialRepository.delete(r);
	}
}
