package br.org.larescolaredencao.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.CriarParceiroDTO;
import br.org.larescolaredencao.model.Parceiro;
import br.org.larescolaredencao.service.ParceiroService;

@RestController
@RequestMapping("/parceiro")
public class ParceiroController {
	
	private ParceiroService parceiroService;
	
	public ParceiroController(ParceiroService parceiroService) {
		this.parceiroService = parceiroService;
	}
	
	@GetMapping("/todos")
	public List<Parceiro> listarParceiros(){
		return parceiroService.getAllParceiros();
	}
	
	@PostMapping("/criar")
	public Parceiro criarParceiro(@RequestBody CriarParceiroDTO criarParceiroDTO) {
		return parceiroService.criarParceiro(criarParceiroDTO);
	}
}
