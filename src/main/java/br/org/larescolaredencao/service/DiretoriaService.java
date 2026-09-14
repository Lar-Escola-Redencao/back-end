package br.org.larescolaredencao.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import br.org.larescolaredencao.dto.AtualizarDiretoriaDTO;
import br.org.larescolaredencao.dto.CriarDiretoriaDTO;
import br.org.larescolaredencao.model.Diretoria;
import br.org.larescolaredencao.repository.DiretoriaRepository;

@Service
public class DiretoriaService {

	private static final String SUBPASTA_FOTOS = "diretoria/";

	private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

	private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/webp", ".webp");

	private final DiretoriaRepository diretoriaRepository;
	private final ArquivoService arquivoService;

	public DiretoriaService(DiretoriaRepository diretoriaRepository, ArquivoService arquivoService) {
		this.diretoriaRepository = diretoriaRepository;
		this.arquivoService = arquivoService;
	}

	public Page<Diretoria> listarTodos(Pageable pageable) {
		return diretoriaRepository.findAll(pageable);
	}

	public Diretoria buscarPorId(Long id) {
		return diretoriaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Membro da diretoria não encontrado"));
	}

	public Diretoria criar(CriarDiretoriaDTO dto) {
		validarFoto(dto.getFoto());
		String caminhoFoto = arquivoService.salvarArquivo(dto.getFoto(), SUBPASTA_FOTOS);
		Diretoria diretoria = new Diretoria();
		diretoria.setNome(dto.getNome());
		diretoria.setCargo(dto.getCargo());
		diretoria.setFoto(caminhoFoto);
		diretoria.setAtivo(true);
		return diretoriaRepository.save(diretoria);
	}

	public Diretoria atualizar(Long id, AtualizarDiretoriaDTO dto) {
		Diretoria diretoria = buscarPorId(id);
		diretoria.setNome(dto.getNome());
		diretoria.setCargo(dto.getCargo());
		if (dto.getAtivo() != null) {
			diretoria.setAtivo(dto.getAtivo());
		}
		if (dto.getFoto() != null && !dto.getFoto().isEmpty()) {
			validarFoto(dto.getFoto());
			String fotoAntiga = diretoria.getFoto();
			diretoria.setFoto(arquivoService.salvarArquivo(dto.getFoto(), SUBPASTA_FOTOS));
			arquivoService.deletarArquivo(fotoAntiga);
		}
		return diretoriaRepository.save(diretoria);
	}

	public void remover(Long id) {
		Diretoria diretoria = buscarPorId(id);
		diretoriaRepository.delete(diretoria);
		arquivoService.deletarArquivo(diretoria.getFoto());
	}

	private void validarFoto(MultipartFile foto) {
		if (foto == null || foto.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto é obrigatória");
		}
		if (foto.getSize() > TAMANHO_MAXIMO_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto deve ter no máximo 5MB");
		}
		String contentType = foto.getContentType();
		if (contentType == null || !TIPOS_PERMITIDOS.containsKey(contentType.toLowerCase())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Formato de foto inválido. Envie um arquivo JPEG, PNG ou WEBP");
		}
	}
}
