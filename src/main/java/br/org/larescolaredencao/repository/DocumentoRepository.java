package br.org.larescolaredencao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.larescolaredencao.model.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
}
