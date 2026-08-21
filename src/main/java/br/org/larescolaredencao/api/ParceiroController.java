package br.org.larescolaredencao.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.AtualizarParceiroDTO;
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
	public List<Parceiro> listarParceiros() {
		return parceiroService.getAllParceiros();
	}

	@GetMapping("/{id}")
	public Parceiro buscarParceiro(@PathVariable Long id) {
		return parceiroService.getParceiroById(id);
	}

	@PostMapping(value = "/criar", consumes = "multipart/form-data")
	public Parceiro criarParceiro(@ModelAttribute CriarParceiroDTO criarParceiroDTO) {
		return parceiroService.criarParceiro(criarParceiroDTO);
	}

	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public Parceiro atualizarParceiro(@PathVariable Long id, @ModelAttribute AtualizarParceiroDTO atualizarParceiroDTO) {
		return parceiroService.atualizarParceiro(id, atualizarParceiroDTO);
	}

	@DeleteMapping("/{id}")
	public void deletarParceiro(@PathVariable Long id) {
		parceiroService.deletarParceiro(id);
	}
}
