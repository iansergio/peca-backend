# Atalhos de categorias no sub-header

## Objetivo

O sub-header mostra só quatro atalhos de categorias, nesta ordem: Telas, Câmeras, Carcaças e Placas.

## Contexto

Visitantes e técnicos usam o sub-header para abrir `/pecas` já filtrado. A mudança acontece só em
`peca-front-end`, na lista `categoriasRapidas` de `components/vitrine/categorias.ts`, e não altera a
API.

## Regras de negócio

As regras estão em [`rules.md`](rules.md): os atalhos exibidos (`R-1`), o grupo Carcaças (`R-2`), a
preservação da lista completa (`R-3`) e a resolução de slugs (`R-4`).

## Requisitos funcionais

- O sub-header exibe Telas, Câmeras, Carcaças e Placas, seguidos do link "Todas".
- O atalho Carcaças abre `/pecas?categoria=carcacas` com as peças de `R-2`.
- Baterias, Películas e Acessórios deixam de ser atalhos.

## Requisitos não funcionais

Sem mudança de contrato, de dados pessoais ou de desempenho. No celular, a barra continua rolando na
horizontal e o toque continua com 44 px de altura.

## Critérios de aceite

- `AC-1`: o sub-header mostra exatamente Telas, Câmeras, Carcaças e Placas, nessa ordem, mais "Todas".
- `AC-2`: o atalho Carcaças lista peças das categorias "Carcaça", "Tampa traseira" e "Aro / Chassi", e
  nenhuma de outra categoria.
- `AC-3`: os atalhos Telas, Câmeras e Placas filtram os mesmos valores de antes.
- `AC-4`: o grid da home e o select do formulário de anúncio continuam com todas as categorias.
- `AC-5`: `/pecas?categoria=baterias`, `peliculas` e `acessorios` continuam filtrando pela categoria.
- `AC-6`: em `/pecas?categoria=carcacas`, o atalho Carcaças fica destacado, com `aria-current="page"`.

## Casos de borda

- `?categoria=carcacas` vindo do grid da home abre o grupo inteiro e destaca o atalho (`R-4`).
- `?categoria=aros-chassis` e `?categoria=tampas-traseiras` seguem filtrando só a própria categoria.
- Slug desconhecido segue sendo ignorado e mostra todas as peças.
- Sem peças no grupo, a página mostra o estado vazio já existente.

## Fora do escopo

Mudar a lista `categorias`, os nomes das categorias, a API ou o filtro por categoria no backend.
