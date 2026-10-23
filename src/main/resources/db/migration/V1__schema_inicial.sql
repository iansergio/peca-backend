-- Schema inicial (T-SQL / Azure SQL Database), espelhando as entidades Usuario, Peca e CodigoVerificacao.
-- O Hibernate roda com ddl-auto=validate: qualquer mudanca nas entidades exige uma nova migration.

CREATE TABLE usuarios (
    id       UNIQUEIDENTIFIER NOT NULL,
    nome     NVARCHAR(120)    NOT NULL,
    email    NVARCHAR(160)    NOT NULL,
    whatsapp NVARCHAR(20)     NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

-- Sem ON DELETE CASCADE: remover um usuario com anuncios falha na FK e a API responde 409.
CREATE TABLE pecas (
    id             UNIQUEIDENTIFIER NOT NULL,
    titulo         NVARCHAR(120)    NOT NULL,
    descricao      NVARCHAR(2000)   NULL,
    preco          DECIMAL(12, 2)   NOT NULL,
    categoria      NVARCHAR(60)     NOT NULL,
    modelo_celular NVARCHAR(80)     NOT NULL,
    imagem_url     NVARCHAR(500)    NULL,
    vendedor_id    UNIQUEIDENTIFIER NOT NULL,
    CONSTRAINT pk_pecas PRIMARY KEY (id),
    CONSTRAINT fk_pecas_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios (id)
);

-- Os codigos OTP sao apagados junto com o usuario.
CREATE TABLE codigos_verificacao (
    id         UNIQUEIDENTIFIER NOT NULL,
    usuario_id UNIQUEIDENTIFIER NOT NULL,
    codigo     NVARCHAR(6)      NOT NULL,
    email      NVARCHAR(160)    NOT NULL,
    expira_em  DATETIME2(6)     NOT NULL,
    utilizado  BIT              NOT NULL,
    tentativas INT              NOT NULL,
    CONSTRAINT pk_codigos_verificacao PRIMARY KEY (id),
    CONSTRAINT fk_codigos_verificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_codigos_verificacao_email ON codigos_verificacao (email);
