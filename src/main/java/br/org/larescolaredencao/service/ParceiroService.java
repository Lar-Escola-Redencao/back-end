package br.org.larescolaredencao.service;

import java.util.List;
import org.springframework.stereotype.Service;

import br.org.larescolaredencao.dto.CriarParceiroDTO;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.repository.ParceiroRepository;

@Service
public class ParceiroService {
	private ParceiroRepository parceiroRepository;
	
	public ParceiroService(ParceiroRepository parceiroRepository) {
		this.parceiroRepository = parceiroRepository;
	}
	
	public List<Parceiro> getAllParceiros() {
		return parceiroRepository.findAll();
	}

	public Parceiro criarParceiro(CriarParceiroDTO criarParceiroDTO) {
		Parceiro p = new Parceiro();
		p.setNome(criarParceiroDTO.getNome());
		p.setLogo(criarParceiroDTO.getLogo());
		p.setAtivo(true);
		return parceiroRepository.save(p);
	}
}