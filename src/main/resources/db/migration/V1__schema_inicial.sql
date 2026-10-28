-- Schema inicial (PostgreSQL), espelhando as entidades Usuario, Peca e CodigoVerificacao.
-- O Hibernate roda com ddl-auto=validate: qualquer mudanca nas entidades exige uma nova migration.

CREATE TABLE usuarios (
    id       UUID         NOT NULL,
    nome     VARCHAR(120) NOT NULL,
    email    VARCHAR(160) NOT NULL,
    whatsapp VARCHAR(20)  NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

-- Sem ON DELETE CASCADE: remover um usuario com anuncios falha na FK e a API responde 409.
CREATE TABLE pecas (
    id             UUID           NOT NULL,
    titulo         VARCHAR(120)   NOT NULL,
    descricao      VARCHAR(2000)  NULL,
    preco          NUMERIC(12, 2) NOT NULL,
    categoria      VARCHAR(60)    NOT NULL,
    modelo_celular VARCHAR(80)    NOT NULL,
    imagem_url     VARCHAR(500)   NULL,
    vendedor_id    UUID           NOT NULL,
    CONSTRAINT pk_pecas PRIMARY KEY (id),
    CONSTRAINT fk_pecas_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios (id)
);

-- Os codigos OTP sao apagados junto com o usuario. usuario_id e nulo enquanto o email ainda nao
-- tem conta: o usuario so e criado quando o codigo e validado (o nome informado fica em nome).
-- criado_em e a base do intervalo entre pedidos e do limite por hora.
CREATE TABLE codigos_verificacao (
    id         UUID         NOT NULL,
    usuario_id UUID         NULL,
    nome       VARCHAR(120) NULL,
    codigo     VARCHAR(6)   NOT NULL,
    email      VARCHAR(160) NOT NULL,
    expira_em  TIMESTAMP(6) NOT NULL,
    criado_em  TIMESTAMP(6) NOT NULL,
    utilizado  BOOLEAN      NOT NULL,
    tentativas INTEGER      NOT NULL,
    CONSTRAINT pk_codigos_verificacao PRIMARY KEY (id),
    CONSTRAINT fk_codigos_verificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_codigos_verificacao_email ON codigos_verificacao (email);
