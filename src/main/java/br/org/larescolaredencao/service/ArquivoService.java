package br.org.larescolaredencao.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.org.larescolaredencao.exception.ApiException;

@Service
public class ArquivoService {

    private static final Logger logger = LoggerFactory.getLogger(ArquivoService.class);

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    public String salvarArquivo(MultipartFile arquivo, String subPasta) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        try {
            Path diretorioPath = Paths.get(uploadDir + subPasta);
            if (!Files.exists(diretorioPath)) {
                Files.createDirectories(diretorioPath);
            }

            String nomeArquivoOriginal = arquivo.getOriginalFilename();
            String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + nomeArquivoOriginal;

            Path caminhoArquivo = diretorioPath.resolve(nomeArquivoUnico);
            Files.copy(arquivo.getInputStream(), caminhoArquivo, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subPasta + nomeArquivoUnico;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar o arquivo.", e);
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
}