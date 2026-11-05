# Fluxos — category-shortcuts

## F-1 — Filtrar pelo atalho Carcaças (caminho feliz)

1. O visitante clica em Carcaças no sub-header (`R-1`).
2. `peca-front-end` abre `/pecas?categoria=carcacas`, e `resolverCategoria` devolve os três valores
   do grupo (`R-2`, `R-4`).
3. `filtrarPecas` mantém só as peças desses valores.
Resultado: a página mostra o título "Carcaças", as peças do grupo e o atalho destacado.

## F-2 — Link antigo de um atalho removido

1. O visitante abre `/pecas?categoria=baterias`.
2. `resolverCategoria` não encontra o atalho e usa a lista `categorias` (`R-3`, `R-4`).
Resultado: a página mostra as baterias, sem atalho destacado.
