-- Limites de solicitacao de OTP e cadastro so apos verificar o email.
-- criado_em: base do cooldown e do limite por hora (linhas antigas recebem o momento da migration).
ALTER TABLE codigos_verificacao
    ADD criado_em DATETIME2(6) NOT NULL
        CONSTRAINT df_codigos_verificacao_criado_em DEFAULT SYSUTCDATETIME();

-- Nome informado por quem ainda nao tem conta; o usuario so e criado quando o codigo e validado.
ALTER TABLE codigos_verificacao ADD nome NVARCHAR(120) NULL;

-- Codigo de email ainda nao cadastrado nao tem usuario. A FK (com ON DELETE CASCADE) e mantida.
ALTER TABLE codigos_verificacao ALTER COLUMN usuario_id UNIQUEIDENTIFIER NULL;
