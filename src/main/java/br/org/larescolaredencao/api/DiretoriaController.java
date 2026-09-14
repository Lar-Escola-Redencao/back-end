package br.org.larescolaredencao.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.dto.AtualizarDiretoriaDTO;
import br.org.larescolaredencao.dto.CriarDiretoriaDTO;
import br.org.larescolaredencao.model.Diretoria;
import br.org.larescolaredencao.service.DiretoriaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/diretoria")
public class DiretoriaController {

	private final DiretoriaService diretoriaService;

	public DiretoriaController(DiretoriaService diretoriaService) {
		this.diretoriaService = diretoriaService;
	}

	@GetMapping("/todos")
	public PagedModel<Diretoria> listarTodos(Pageable pageable) {
		return new PagedModel<>(diretoriaService.listarTodos(pageable));
	}

	@GetMapping("/{id}")
	public Diretoria buscarPorId(@PathVariable("id") Long id) {
		return diretoriaService.buscarPorId(id);
	}

	@PostMapping(value = "/criar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Diretoria> criar(@Valid @ModelAttribute CriarDiretoriaDTO dto) {
		Diretoria criado = diretoriaService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Diretoria atualizar(@PathVariable("id") Long id, @Valid @ModelAttribute AtualizarDiretoriaDTO dto) {
		return diretoriaService.atualizar(id, dto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable("id") Long id) {
		diretoriaService.remover(id);
		return ResponseEntity.noContent().build();
	}
}
