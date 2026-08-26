package br.org.larescolaredencao.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArquivoService {

    private static final Logger logger = LoggerFactory.getLogger(ArquivoService.class);

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public String salvarArquivo(MultipartFile arquivo, String subPasta, TipoArquivo tipo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        validarTipoArquivo(arquivo, tipo);

        try {
            Path diretorioPath = Paths.get(uploadDir + subPasta).normalize();
            if (!Files.exists(diretorioPath)) {
                Files.createDirectories(diretorioPath);
            }

            String nomeBase = Paths.get(obterNomeOriginalSeguro(arquivo)).getFileName().toString();
            String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + nomeBase;

            Path caminhoArquivo = diretorioPath.resolve(nomeArquivoUnico).normalize();
            if (!caminhoArquivo.startsWith(diretorioPath)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
            }

            Files.copy(arquivo.getInputStream(), caminhoArquivo, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subPasta + nomeArquivoUnico;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o arquivo.", e);
        }
    }

    public void deletarArquivo(String caminhoNoBanco) {
        if (caminhoNoBanco == null || caminhoNoBanco.trim().isEmpty()) {
            return;
        }

        try {
            String caminhoRelativo = caminhoNoBanco.startsWith("/uploads/")
                                     ? caminhoNoBanco.substring(9)
                                     : caminhoNoBanco;
            Path caminhoArquivo = Paths.get(uploadDir, caminhoRelativo);
            Files.deleteIfExists(caminhoArquivo);
        } catch (IOException e) {
            logger.warn("Falha ao deletar o arquivo físico: {}", caminhoNoBanco, e);
        }
    }

    public Resource carregarComoRecurso(String caminhoNoBanco) {
        try {
            String caminhoRelativo = caminhoNoBanco.startsWith("/uploads/")
                                     ? caminhoNoBanco.substring(9)
                                     : caminhoNoBanco;
            Path diretorioBase = Paths.get(uploadDir).normalize();
            Path caminhoArquivo = diretorioBase.resolve(caminhoRelativo).normalize();

            if (!caminhoArquivo.startsWith(diretorioBase)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de arquivo inválido.");
            }

            Resource recurso = new UrlResource(caminhoArquivo.toUri());
            if (recurso.exists() && recurso.isReadable()) {
                return recurso;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no armazenamento.");
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de arquivo inválido.");
        }
    }

    public void validarTipoArquivo(MultipartFile arquivo, TipoArquivo tipo) {
        String nomeOriginal = obterNomeOriginalSeguro(arquivo);
        String extensao = extrairExtensao(nomeOriginal);
        String mensagemErro = "Tipo de arquivo não permitido. Extensões aceitas: "
                + String.join(", ", tipo.getExtensoesPermitidas()).toUpperCase(Locale.ROOT) + ".";

        if (!tipo.getExtensoesPermitidas().contains(extensao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagemErro);
        }

        String tipoMime = arquivo.getContentType();
        if (tipoMime == null || !tipo.getTiposMimePermitidos().contains(tipoMime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagemErro);
        }
    }

    public String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null) {
            return "";
        }
        int posicaoPonto = nomeArquivo.lastIndexOf('.');
        if (posicaoPonto == -1 || posicaoPonto == nomeArquivo.length() - 1) {
            return "";
        }
        return nomeArquivo.substring(posicaoPonto + 1).toLowerCase(Locale.ROOT);
    }

    public String sanitizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replaceAll("[\\r\\n\"\\\\]", "").trim();
    }

    private String obterNomeOriginalSeguro(MultipartFile arquivo) {
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do arquivo inválido.");
        }
        return nomeOriginal;
    }
}
