package br.org.larescolaredencao.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class OrdenacaoPaginada {

    private OrdenacaoPaginada() {
    }

    static Pageable comPadrao(Pageable pagina, Sort padrao) {
        Sort ordem = pagina.getSortOr(padrao);
        if (ordem.getOrderFor("id") == null) {
            ordem = ordem.and(Sort.by(Sort.Order.desc("id")));
        }
        return comOrdem(pagina, ordem);
    }

    static Pageable comOrdem(Pageable pagina, Sort ordem) {
        return pagina.isPaged()
                ? PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(), ordem)
                : Pageable.unpaged(ordem);
    }

    static Pageable porCadastro(Pageable pagina) {
        return comPadrao(pagina, Sort.by(Sort.Order.desc("id")));
    }
}
