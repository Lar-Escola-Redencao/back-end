package br.org.larescolaredencao.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

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
	public PagedModel<Parceiro> listarParceiros(Pageable pageable) {
		return new PagedModel<>(parceiroService.getAllParceiros(pageable));
	}

	@GetMapping("/{id}")
	public Parceiro buscarParceiro(@PathVariable("id") Long id) {
		return parceiroService.getParceiroById(id);
	}

	@PostMapping(value = "/criar", consumes = "multipart/form-data")
	public Parceiro criarParceiro(@Valid @ModelAttribute CriarParceiroDTO criarParceiroDTO) {
		return parceiroService.criarParceiro(criarParceiroDTO);
	}

	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public Parceiro atualizarParceiro(@PathVariable("id") Long id, @ModelAttribute AtualizarParceiroDTO atualizarParceiroDTO) {
		return parceiroService.atualizarParceiro(id, atualizarParceiroDTO);
	}

	@DeleteMapping("/{id}")
	public void deletarParceiro(@PathVariable("id") Long id) {
		parceiroService.deletarParceiro(id);
	}
}
