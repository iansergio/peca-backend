# Regras de negócio — listing-management

Status: reverse-engineered from code on 2026-11-05

## Dados do anúncio

### R-1 — Campos obrigatórios e limites (observed)

A API exige `titulo` (até 120), `preco`, `categoria` (até 60), `modeloCelular` (até 80) e `whatsapp`.
`descricao` é opcional (até 2000). `titulo`, `categoria` e `modeloCelular` são gravados sem espaços nas
pontas. Dados inválidos respondem 400.

### R-2 — Preço (observed)

O preço vai de 0,01 a 10 dígitos inteiros, com até 2 casas decimais. O formulário aceita vírgula ou
ponto como separador decimal.

### R-3 — WhatsApp do anúncio (observed)

O número tem de 10 a 13 dígitos. O formulário aceita máscara e remove o que não for dígito.

### R-4 — Um contato por pessoa (confirmed by the user on 2026-11-05)

O contato do anúncio é o WhatsApp do perfil. Criar ou editar um anúncio grava o número em
`usuarios.whatsapp`, o que muda o contato de todos os anúncios da pessoa.

### R-5 — Categorias válidas (confirmed by the user on 2026-11-05)

As categorias válidas são as 22 da lista `categorias` do front. O formulário só aceita essas; a API
ainda não valida, como descreve `D-3`.

## Autoria

### R-6 — Vendedor (observed)

O vendedor é sempre o usuário do token. O corpo da requisição não informa nem altera o vendedor.

### R-7 — Só o vendedor altera (observed)

Editar, excluir e enviar foto exigem que o token seja do vendedor; caso contrário, a API responde 403,
sem efeito. A checagem fica em `PecaService`. A que existe no front serve só para a experiência de uso.

### R-8 — Publicação imediata (observed)

O anúncio aparece na vitrine assim que é criado. Não há moderação nem estados como rascunho ou pausado.

## Foto

### R-9 — Uma foto por anúncio (observed)

Cada anúncio guarda uma `imagemUrl`. Enviar uma foto substitui a anterior, cujo arquivo é apagado.

### R-10 — Formatos e tamanho (observed)

A API aceita JPEG, PNG ou WEBP, identificados pelo conteúdo do arquivo, com até 5 MB. O nome do arquivo
é gerado pelo servidor.

### R-11 — Foto no formulário (observed)

O formulário exige foto ao publicar. Ao editar, deixar o campo vazio mantém a foto atual.

### R-12 — Falha no upload (observed)

Criar e enviar a foto são chamadas separadas. Se só a foto falhar, os dados ficam salvos e o front mostra
um aviso. Ao criar, o anúncio fica publicado sem foto, o que é o defeito `D-1`.

## Exclusão

### R-13 — Exclusão definitiva (observed)

Excluir remove o anúncio e o arquivo da foto, sem como desfazer. No front, a exclusão exige confirmação,
e receber 404 conta como sucesso.

## Meus anúncios

### R-14 — Lista do vendedor (observed)

A aba busca a listagem pública inteira e mantém os anúncios cujo `vendedor.id` é o `sub` do token.

## Questões em aberto

- `Q-1`: não há limite para criar anúncios ou enviar fotos, então um usuário autenticado pode encher o
  disco. O front trata 429 nessas rotas, mas a API nunca responde 429 nelas. Deve existir um limite?
- `Q-2`: em edições simultâneas, a última gravação vale sem aviso. Isso é aceitável?

## Defeitos observados

Comportamento registrado, não corrigido. Para corrigir, use `/peca.fix` ou `/peca.start`.

- `D-1`: **anúncio sem foto** (classificado como defeito pelo usuário em 2026-11-05). A foto é exigida
  só no formulário: a API aceita criar sem ela, e uma falha no upload deixa o anúncio publicado sem foto.
- `D-2`: **anúncio sem contato** (classificado como defeito pelo usuário em 2026-11-05). `UsuarioRequest`
  aceita `whatsapp` nulo, então `PUT /api/usuarios/{id}` pode apagar o número, e os anúncios ficam sem
  contato. Só acontece chamando a API: o formulário de perfil exige o número. O comentário em
  `lib/types.ts` sugere que a pessoa consegue apagar o número, o que não vale para a tela.
- `D-3`: **categoria fora da lista** (classificado como defeito pelo usuário em 2026-11-05). A API aceita
  qualquer texto. Esse anúncio pode não aparecer nos filtros, e o formulário de edição só salva depois
  de trocar a categoria. Ver P-11 em `docs/Architecture.md`.
- `D-4`: **arquivo apagado antes do commit.** `PecaService.deletar` e `enviarImagem` removem o arquivo
  dentro da transação. Se o commit falhar, o registro aponta para uma foto que não existe mais, e em
  `enviarImagem` o arquivo novo fica órfão. Ver P-9.
- `D-5`: **caminho interno no aviso.** Em falha de gravação, a API inclui a mensagem do `IOException` no
  `detail`, e o front mostra esse texto no toast de publicação parcial. Ver P-7.
- `D-6`: **Meus anúncios depende da listagem pública.** Sem paginação, a aba baixa todos os anúncios para
  filtrar no servidor do front. Ver P-10.
- `D-7`: **pasta vazia no disco.** Excluir apaga o arquivo, mas mantém a pasta `<diretorio>/<id-do-anuncio>`.
