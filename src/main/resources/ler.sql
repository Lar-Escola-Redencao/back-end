DROP DATABASE IF EXISTS ler;
CREATE DATABASE ler;
USE ler;

-- ==========================================
-- TABELAS DA GESTÃO INTERNA
-- ==========================================

-- 1. Papéis (Administrador, Coordenador, Monitor)
CREATE TABLE papel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome_papel VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);

-- 2. Unidades
CREATE TABLE unidade (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    endereco VARCHAR(255) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    dias_funcionamento VARCHAR(150) NOT NULL,
    idade_min INT NOT NULL,
    idade_max INT NOT NULL,
    cor_hex VARCHAR(7) DEFAULT '#F5F5F5' 
);

-- 3. Membros (Usuários do sistema interno)
CREATE TABLE membro (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome_completo VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    endereco VARCHAR(255),
    telefone VARCHAR(20),
    id_papel INT NOT NULL,
    CONSTRAINT fk_membro_papel FOREIGN KEY (id_papel) REFERENCES papel(id)
);

-- 4. Intermediária: Membro atua em Unidade
CREATE TABLE membro_unidade (
    id_membro INT NOT NULL,
    id_unidade INT NOT NULL,
    PRIMARY KEY (id_membro, id_unidade),
    CONSTRAINT fk_mu_membro FOREIGN KEY (id_membro) REFERENCES membro(id) ON DELETE CASCADE,
    CONSTRAINT fk_mu_unidade FOREIGN KEY (id_unidade) REFERENCES unidade(id) ON DELETE CASCADE
);

-- 5. Turmas
CREATE TABLE turma (
    id INT AUTO_INCREMENT PRIMARY KEY,
    periodo ENUM('MANHA', 'TARDE') NOT NULL,
    horario_inicio TIME NOT NULL,
    horario_fim TIME NOT NULL,
    id_unidade INT NOT NULL,
    CONSTRAINT fk_turma_unidade FOREIGN KEY (id_unidade) REFERENCES unidade(id)
);

-- ============================================================================
-- 6. Assistidos (Dados Pessoais e Imutáveis)
-- ============================================================================
-- JUSTIFICATIVA: 
-- A tabela de assistidos agora armazena estritamente os dados físicos e civis 
-- da criança. As informações voláteis de vínculo (como a turma e o status) 
-- foram removidas para a tabela de 'matricula' a fim de evitar a perda de 
-- histórico caso o assistido se afaste e retorne à OSC no futuro.
-- ============================================================================
CREATE TABLE assistido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome_completo VARCHAR(150) NOT NULL,
    data_nascimento DATE NOT NULL,
    cpf VARCHAR(14) UNIQUE,
    documento_auxiliar VARCHAR(100),
    tipo_documento ENUM('CERTIDAO_NASCIMENTO', 'RG', 'OUTRO'),
    imagem_perfil VARCHAR(255)
);

-- ============================================================================
-- 6.1. Matrícula (O Vínculo Temporal e Histórico)
-- ============================================================================
-- JUSTIFICATIVA:
-- Resolve o problema da "temporalidade". Separar quem é a pessoa (assistido) 
-- do seu vínculo atual (matrícula) permite que o mesmo menino tenha múltiplas 
-- passagens pela instituição. Se ele "trancar" e voltar no ano seguinte, ganha 
-- uma nova matrícula, preservando 100% da integridade dos relatórios de 
-- frequência e ocorrências gerados no passado.
-- ============================================================================
CREATE TABLE matricula (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_assistido INT NOT NULL,
    id_turma INT NOT NULL,
    status ENUM('ATIVO', 'INATIVO', 'EGRESSO') DEFAULT 'ATIVO',
    data_ingresso DATE NOT NULL DEFAULT (CURRENT_DATE),
    data_desligamento DATE DEFAULT NULL,
    CONSTRAINT fk_matricula_assistido FOREIGN KEY (id_assistido) REFERENCES assistido(id) ON DELETE CASCADE,
    CONSTRAINT fk_matricula_turma FOREIGN KEY (id_turma) REFERENCES turma(id)
);

-- ============================================================================
-- 6.2. Histórico de Período Integral (Exceções de Turno Temporária)
-- ============================================================================
-- JUSTIFICATIVA:
-- Substitui a flag booleana de "tempo_integral". Usar datas de início e fim 
-- automatiza as listas de frequência, permitindo que a criança "apareça" na 
-- chamada do contraturno oposto apenas dentro daquela janela exata (ex: Férias 
-- de julho), sem exigir intervenção diária manual da coordenação e mantendo 
-- a rastreabilidade exigida.
-- ============================================================================
CREATE TABLE periodo_integral (
    id INT AUTO_INCREMENT PRIMARY KEY,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    motivo VARCHAR(150),
    id_matricula INT NOT NULL, 
    CONSTRAINT fk_pi_matricula FOREIGN KEY (id_matricula) REFERENCES matricula(id) ON DELETE CASCADE
);

-- 7. Contatos
CREATE TABLE contato (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome_completo VARCHAR(150) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(100)
);

-- 8. Intermediária: Assistido e Contato
CREATE TABLE contato_assistido (
    id_assistido INT NOT NULL,
    id_contato INT NOT NULL,
    parentesco VARCHAR(50) NOT NULL,
    PRIMARY KEY (id_assistido, id_contato),
    CONSTRAINT fk_ca_assistido FOREIGN KEY (id_assistido) REFERENCES assistido(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_contato FOREIGN KEY (id_contato) REFERENCES contato(id) ON DELETE CASCADE
);

-- 9. Frequência
CREATE TABLE frequencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    data_registro DATE NOT NULL,
    presente BOOLEAN NOT NULL,
    id_matricula INT NOT NULL,
    id_membro INT NOT NULL, 
    CONSTRAINT fk_freq_matricula FOREIGN KEY (id_matricula) REFERENCES matricula(id) ON DELETE CASCADE,
    CONSTRAINT fk_freq_membro FOREIGN KEY (id_membro) REFERENCES membro(id)
);

-- 10. Ocorrências
CREATE TABLE ocorrencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    data_hora DATETIME NOT NULL,
    descricao TEXT NOT NULL,
    tipo_ocorrencia ENUM('COMPORTAMENTO', 'SAUDE', 'ASSISTENCIA') NOT NULL,
    id_matricula INT NOT NULL,
    id_membro INT NOT NULL,  
    CONSTRAINT fk_ocorr_matricula FOREIGN KEY (id_matricula) REFERENCES matricula(id) ON DELETE CASCADE,
    CONSTRAINT fk_ocorr_membro FOREIGN KEY (id_membro) REFERENCES membro(id)
);

-- ==========================================
-- TABELAS DA ÁREA PÚBLICA (CMS / Transparência)
-- ==========================================

-- 11. Parceiros
CREATE TABLE parceiro (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    logo VARCHAR(255) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE
);

-- 12. Eventos
CREATE TABLE evento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    data_evento DATETIME NOT NULL,
    endereco VARCHAR(255) NOT NULL,
    imagem VARCHAR(255) NOT NULL,
    valor DECIMAL(10,2) DEFAULT 0.00,
    tipo_evento ENUM('ARRECADACAO', 'CULTURAL', 'ESPORTIVO', 'COMEMORATIVO') NOT NULL,
    comentario_pos_evento TEXT 
);

-- 12.1. Mídias do Pós-Evento (Fotos e Vídeos vinculados ao Evento)
CREATE TABLE midia_evento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    url_midia VARCHAR(255) NOT NULL, 
    tipo_midia ENUM('IMAGEM', 'VIDEO') NOT NULL,
    id_evento INT NOT NULL,
    CONSTRAINT fk_midia_evento FOREIGN KEY (id_evento) REFERENCES evento(id) ON DELETE CASCADE
);

-- 13. Intermediária: Evento possui (ou não) Parceiros
CREATE TABLE parceiro_evento (
    id_evento INT NOT NULL,
    id_parceiro INT NOT NULL,
    PRIMARY KEY (id_evento, id_parceiro),
    CONSTRAINT fk_ep_evento FOREIGN KEY (id_evento) REFERENCES evento(id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_parceiro FOREIGN KEY (id_parceiro) REFERENCES parceiro(id) ON DELETE CASCADE
);

-- 14. Páginas
CREATE TABLE pagina (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE
);

-- 15. Seções
CREATE TABLE secao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    conteudo TEXT,
    imagem VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    id_pagina INT NOT NULL,
    CONSTRAINT fk_secao_pagina FOREIGN KEY (id_pagina) REFERENCES pagina(id) ON DELETE CASCADE
);

-- 16. Documentos (Para a Transparência)
CREATE TABLE documento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    arquivo VARCHAR(255) NOT NULL,
    id_secao INT NOT NULL,
    CONSTRAINT fk_doc_secao FOREIGN KEY (id_secao) REFERENCES secao(id) ON DELETE CASCADE
);

-- 17. Composição Administrativa / Diretoria
CREATE TABLE diretoria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cargo VARCHAR(100) NOT NULL,
    foto VARCHAR(255) NOT NULL, 
    ativo BOOLEAN DEFAULT TRUE
);

-- 18. Redes Sociais
CREATE TABLE rede_social (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    url VARCHAR(255) NOT NULL,
    icone VARCHAR(100) NOT NULL, 
    ativo BOOLEAN DEFAULT TRUE
);

-- ==========================================
-- TABELAS DO SPRING ENVERS (AUDITORIA)
-- ==========================================

-- 19. Tabela central de revisões do Envers (Obrigatória)
CREATE TABLE revinfo (
    rev INT AUTO_INCREMENT PRIMARY KEY,
    revtstmp BIGINT NOT NULL,
    id_membro INT, 
    CONSTRAINT fk_revinfo_membro FOREIGN KEY (id_membro) REFERENCES membro(id)
);

-- ==========================================
-- INSERTS DE EXEMPLO E VALIDAÇÃO
-- ==========================================

INSERT INTO papel (nome_papel, descricao) VALUES 
('ADMINISTRADOR', 'Manutenção de conteúdo público, unidades, turmas e gestão completa de usuários.');
-- ('COORDENADOR', 'Gestão operacional de assistidos, monitores e controle de frequências/ocorrências.'),
-- ('MONITOR', 'Acompanhamento diário, registro de frequências e ocorrências pedagógicas/comportamentais.');

INSERT INTO membro (nome_completo, email, senha, cpf, endereco, telefone, id_papel) VALUES
('Administrador Master', 'admin@larredencao.org.br', '$2a$10$wT8v/mJz/Q281Bw20h0D8.N3mH.4u8xO9d6P.lWlY0KqX3y79T7aO', '00000000000', 'Sede - São José', '(16) 99999-9999', 1),
('Ana', 'ana@gmail.com', '$2b$10$u3a0nXGUsX0/7KrF1Ew2MO/UqOOpOINlNvpT7BWEEtBhdOOkSDE.W', '11111111111', 'Sede - São José', '(16) 99999-9999', 1);

-- INSERT INTO unidade (nome, endereco, telefone, email, dias_funcionamento, idade_min, idade_max, cor_hex) VALUES 
-- ('Sede - São José', 'Av. Francisco Sampaio Peixoto, 372', '(16) 3322-6923', 'larescolaredencaosede@gmail.com', 'Seg a Sex das 06h às 18h', 6, 14, '#3A7D2C'), 
-- ('Bezerra de Menezes - Yolanda', 'Av. Valdomiro Blundi, 519 - Yolanda Ópice', '(00) 0000-0000', 'lerbezerra@gmail.com', 'Seg a Sex das 06h às 18h', 6, 14, '#092E5E'), 
-- ('SOS Bombeiros - Vila Xavier', 'Rua Cândido Portinari, 123 - Jardim Veridiana - Vl. Xavier', '(16) 3335-8969', 'sosbomb@gmail.com', 'Seg a Sex das 06h às 18h', 6, 14, '#C31F26'); 