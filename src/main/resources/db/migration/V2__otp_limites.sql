-- Limites de solicitacao de OTP e cadastro so apos verificar o email.
-- criado_em: base do cooldown e do limite por hora (linhas antigas recebem o momento da migration).
-- now() e convertido para o fuso da sessao; no Supabase o padrao e UTC, como o resto da aplicacao.
-- No SQL Server o default se chamava df_codigos_verificacao_criado_em; no PostgreSQL default nao e
-- constraint e nao tem nome (o nome seria descartado, e o H2 dos testes nem aceita a sintaxe).
ALTER TABLE codigos_verificacao
    ADD COLUMN criado_em TIMESTAMP(6) NOT NULL DEFAULT now();

-- Nome informado por quem ainda nao tem conta; o usuario so e criado quando o codigo e validado.
ALTER TABLE codigos_verificacao ADD COLUMN nome VARCHAR(120) NULL;

-- Codigo de email ainda nao cadastrado nao tem usuario. A FK (com ON DELETE CASCADE) e mantida.
ALTER TABLE codigos_verificacao ALTER COLUMN usuario_id DROP NOT NULL;
