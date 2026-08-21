package br.org.larescolaredencao.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.repository.PapelRepository;

@Service
public class PapelService {

    private final PapelRepository papelRepository;

    public PapelService(PapelRepository papelRepository) {
        this.papelRepository = papelRepository;
    }

    public List<Papel> getAllPapeis() {
        return papelRepository.findAll();
    }
}