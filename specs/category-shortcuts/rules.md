# Regras de negócio — category-shortcuts

## R-1 — Atalhos exibidos

O sub-header mostra só os atalhos Telas, Câmeras, Carcaças e Placas, nessa ordem. Baterias, Películas
e Acessórios não são atalhos.

## R-2 — Grupo Carcaças

O atalho Carcaças agrupa os valores da API "Carcaça", "Tampa traseira" e "Aro / Chassi". Os demais
atalhos mantêm os valores atuais: Telas ("Tela"), Câmeras ("Câmera", "Lente de câmera") e Placas
("Placa principal", "Subplaca").

## R-3 — Lista completa preservada

A lista `categorias` não muda: o grid da home, a página "Todas", o select e a validação do formulário
de anúncio continuam com todas as categorias.

## R-4 — Resolução de slugs

Um atalho tem prioridade sobre a categoria de mesmo slug, como já ocorre com `cameras`. Por isso, o
card Carcaças do grid também abre o grupo de `R-2`. Slugs que deixaram de ser atalho continuam
resolvidos pela lista `categorias`.
