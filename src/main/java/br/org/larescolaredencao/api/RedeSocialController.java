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

import jakarta.validation.Valid;

import br.org.larescolaredencao.dto.AtualizarRedeSocialDTO;
import br.org.larescolaredencao.dto.CriarRedeSocialDTO;
import br.org.larescolaredencao.model.RedeSocial;
import br.org.larescolaredencao.service.RedeSocialService;

@RestController
@RequestMapping("/rede-social")
public class RedeSocialController {

	private RedeSocialService redeSocialService;

	public RedeSocialController(RedeSocialService redeSocialService) {
		this.redeSocialService = redeSocialService;
	}

	@GetMapping("/todas")
	public List<RedeSocial> listarRedesSociais() {
		return redeSocialService.getAllRedesSociais();
	}

	@GetMapping("/{id}")
	public RedeSocial buscarRedeSocial(@PathVariable Long id) {
		return redeSocialService.getRedeSocialById(id);
	}

	@PostMapping(value = "/criar", consumes = "multipart/form-data")
	public RedeSocial criarRedeSocial(@Valid @ModelAttribute CriarRedeSocialDTO criarRedeSocialDTO) {
		return redeSocialService.criarRedeSocial(criarRedeSocialDTO);
	}

	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public RedeSocial atualizarRedeSocial(@PathVariable Long id, @ModelAttribute AtualizarRedeSocialDTO atualizarRedeSocialDTO) {
		return redeSocialService.atualizarRedeSocial(id, atualizarRedeSocialDTO);
	}

	@DeleteMapping("/{id}")
	public void deletarRedeSocial(@PathVariable Long id) {
		redeSocialService.deletarRedeSocial(id);
	}
}
