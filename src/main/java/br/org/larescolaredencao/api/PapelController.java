package br.org.larescolaredencao.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.org.larescolaredencao.model.Papel;
import br.org.larescolaredencao.service.PapelService;

@RestController
@RequestMapping("/papel")
public class PapelController {

    private final PapelService papelService;

    public PapelController(PapelService papelService) {
        this.papelService = papelService;
    }

    @GetMapping("/todos")
    public List<Papel> listarPapeis() {
        return papelService.getAllPapeis();
    }
}