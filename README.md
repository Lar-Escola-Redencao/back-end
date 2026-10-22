# Lar Escola Redenção | Back-end

API do sistema de gestão operacional e institucional desenvolvido para o **Lar Escola Redenção**, organização da sociedade civil sem fins lucrativos de Araraquara (SP). O projeto é realizado por sete estudantes de Tecnologia em Sistemas para Internet do Instituto Federal de São Paulo (IFSP), Campus Araraquara, no âmbito de atividades acadêmicas de extensão.

O objetivo é apoiar a organização de informações e rotinas da instituição, que atende crianças e adolescentes em situação de vulnerabilidade social. O levantamento de requisitos identificou a necessidade de centralizar cadastros, matrículas, frequências, ocorrências e informações institucionais antes distribuídas entre registros manuais, documentos e aplicativos de mensagens.

**Estado do projeto:** desenvolvimento ativo, com entrega prevista para o final de 2026. Há uma implantação parcial destinada a treinamento e futura avaliação com a instituição. A validação operacional ainda não foi concluída.

## Contexto social

Fundado em **29 de agosto de 1978**, o Lar Escola Redenção realiza ações socioeducativas, culturais e esportivas e atividades de fortalecimento de vínculos familiares e comunitários. Atua na sede São José, no Núcleo Bezerra de Menezes e no projeto SOS Bombeiros no Resgate da Cidadania.

A trajetória da organização pode ser consultada no [verbete da Wikipédia](https://pt.wikipedia.org/wiki/Lar_Escola_Reden%C3%A7%C3%A3o).

## Escopo da API

Os requisitos e o planejamento de desenvolvimento compreendem:

| Módulo | Finalidade |
| --- | --- |
| Colaboradores e permissões | Gerenciar contas internas e seus níveis de acesso |
| Unidades e turmas | Organizar locais, períodos e grupos de atendimento |
| Usuários e matrículas | Manter dados cadastrais, responsáveis e vínculos com turmas |
| Diário de turma | Registrar presenças, ausências e ocorrências |
| Conteúdo institucional | Administrar informações, parceiros, eventos e documentos públicos |
| Relatórios e auditoria | Apoiar a consulta, exportação e rastreabilidade de registros |

A tabela descreve o escopo planejado e desenvolvido por etapas. Nem todos os módulos estão necessariamente concluídos ou homologados. Processamento integrado de doações e funcionalidades comerciais da gráfica não fazem parte do núcleo inicial priorizado.

## Arquitetura e tecnologias

| Componente | Tecnologia |
| --- | --- |
| API | Java, Spring Boot e HTTP/JSON |
| Persistência | MySQL e Spring Data JPA |
| Segurança | Spring Security e autenticação baseada em JWT |
| Organização do código | Controllers, services, repositories, models e DTOs |
| Front-end | Angular em repositório separado |

As decisões de arquitetura foram documentadas durante o levantamento de requisitos e a modelagem do sistema. A implementação disponível deve ser conferida na versão atual do código.

## Desenvolvimento local

```bash
git clone https://github.com/Lar-Escola-Redencao/back-end.git
cd back-end
```

Para executar, consulte o arquivo de build e configure o banco MySQL e o perfil de desenvolvimento. 
## Segurança e privacidade

O sistema foi projetado para lidar com registros que podem incluir informações pessoais e socioeconômicas de crianças e famílias. Controle de acesso por perfil, rastreabilidade de alterações e proteção de informações são requisitos centrais do trabalho.

Não inclua dados reais de usuários, exportações de bancos de dados, tokens ou credenciais em commits e issues públicas. A conformidade com a LGPD depende de avaliação e validação efetivas; ela não deve ser presumida apenas pelas tecnologias adotadas.

Em caso de possível vulnerabilidade com exposição de dados, contate os mantenedores por um canal privado antes de divulgar detalhes técnicos.

## Contexto acadêmico

O sistema integra uma atividade de extensão do IFSP, desenvolvida a partir de diálogo e levantamento de necessidades junto ao Lar Escola Redenção. A equipe organiza o trabalho em sprints, com práticas de engenharia de software, gestão de projetos e colaboração entre áreas.

Um **relato de experiência acadêmico está em elaboração**, com análise do processo de desenvolvimento, do uso do Scrum e de contribuições teóricas de Edgar Morin. O texto é preparado para possível submissão à revista *Extensão em Foco*.

## Interface e contribuições

A interface pública, o painel administrativo e as capturas de tela estão no repositório [Lar-Escola-Redencao/front-end](https://github.com/Lar-Escola-Redencao/front-end).

## Licença

Código disponibilizado sob a [licença MIT](LICENSE).

## English summary

Java and Spring Boot API developed by seven IFSP students for Lar Escola Redenção, a Brazilian nonprofit organization. The open-source project supports an institutional website and internal administrative records. It remains under development, with institutional evaluation and final delivery planned for 2026.
