package br.org.larescolaredencao.audit;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import br.org.larescolaredencao.model.ArquivoSaude;
import br.org.larescolaredencao.model.ComposicaoFamiliar;
import br.org.larescolaredencao.model.Contato;
import br.org.larescolaredencao.model.ContatoUsuario;
import br.org.larescolaredencao.model.FichaSocioeconomica;
import br.org.larescolaredencao.model.Frequencia;
import br.org.larescolaredencao.model.Matricula;
import br.org.larescolaredencao.model.Membro;
import br.org.larescolaredencao.model.Ocorrencia;
import br.org.larescolaredencao.model.Usuario;
import br.org.larescolaredencao.model.enums.TipoRegistroHistorico;
import static br.org.larescolaredencao.model.enums.TipoRegistroHistorico.*;

public enum EntidadeAuditada {
    MEMBRO(Membro.class, "membro", COLABORADORES, "Colaborador", "id"),
    USUARIO(Usuario.class, "usuario", USUARIOS, "Cadastro do usuário", "id"),
    MATRICULA(Matricula.class, "matricula", USUARIOS, "Matrícula", "id"),
    FICHA(FichaSocioeconomica.class, "ficha_socioeconomica", USUARIOS, "Dados socioeconômicos", "id_usuario"),
    COMPOSICAO(ComposicaoFamiliar.class, "composicao_familiar", USUARIOS, "Composição familiar", "id"),
    ARQUIVO(ArquivoSaude.class, "arquivo_saude", USUARIOS, "Arquivo de saúde", "id"),
    CONTATO(Contato.class, "contato", CONTATOS, "Contato", "id"),
    VINCULO(ContatoUsuario.class, "contato_usuario", VINCULOS_CONTATOS, "Vínculo de contato", "id_usuario", "id_contato"),
    FREQUENCIA(Frequencia.class, "frequencia", PRESENCAS, "Presença", "id"),
    OCORRENCIA(Ocorrencia.class, "ocorrencia", OCORRENCIAS, "Ocorrência", "id");

    public final Class<?> classe;
    public final String tabela;
    public final TipoRegistroHistorico categoria;
    public final String descricao;
    public final List<String> chaves;

    EntidadeAuditada(Class<?> classe, String tabela, TipoRegistroHistorico categoria, String descricao, String... chaves) {
        this.classe = classe;
        this.tabela = tabela;
        this.categoria = categoria;
        this.descricao = descricao;
        this.chaves = List.of(chaves);
    }

    public static Optional<EntidadeAuditada> porClasse(Class<?> classe) {
        return Arrays.stream(values()).filter(entidade -> entidade.classe.isAssignableFrom(classe)).findFirst();
    }
}
