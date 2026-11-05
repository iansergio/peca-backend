# Modelo de especificação

Toda especificação de comportamento do sistema nasce deste modelo e fica em
`/specs/<feature>/spec.md`. Numere os critérios de aceite (`AC-1`, `AC-2`, …).

## Objetivo

Descreva o resultado visível ao usuário ou ao sistema pelo qual esta especificação responde.

## Contexto

Explique por que este comportamento existe, quem depende dele (atores) e quais apps, módulos e
sistemas externos participam.

## Regras de negócio

Resuma as regras e aponte para `rules.md`, onde cada uma é numerada (`R-n`). Não coloque regras de
negócio em código, adaptadores, clientes gerados, UI ou testes sem referenciar uma regra daqui.

## Requisitos funcionais

Descreva o comportamento exigido em termos observáveis (contratos de API, comportamento da UI,
efeitos colaterais).

## Requisitos não funcionais

Desempenho, confiabilidade, segurança, auditoria, compatibilidade, proteção de dados e operação.
Fluxos que gravam vários registros relacionados devem declarar a consistência e a transação
esperadas. Todo campo pessoal novo é classificado aqui conforme a LGPD.

## Critérios de aceite

Critérios verificáveis que a implementação e os testes precisam atender. Uma linha cada, numerados
`AC-n`.

## Casos de borda

Condições de limite, falhas, entradas inválidas, novas tentativas, idempotência, eventos
duplicados, integrações indisponíveis, concorrência e conflitos de dados.

## Fora do escopo

Declare explicitamente o que esta especificação não define.
