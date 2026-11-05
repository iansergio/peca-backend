# Gestão de anúncios

Status: reverse-engineered from code on 2026-11-05

Descreve o que o sistema faz hoje. Comportamento considerado errado está em
[`rules.md`](rules.md#defeitos-observados), não aqui como requisito.

## Objetivo

Um técnico autenticado publica peças, edita ou exclui os próprios anúncios, envia ou troca a foto e
vê seus anúncios no perfil.

## Contexto

Os atores são o técnico autenticado (vendedor) e, nas tentativas recusadas, qualquer outro usuário.
Participam `peca-front-end` (`/anunciar`, `/pecas/[id]/editar`, `/perfil?aba=anuncios`) e
`peca-backend` (pacotes `peca` e `storage`), com PostgreSQL e disco local.

Vitrine, busca, detalhe e contato pelo WhatsApp usam estes dados, mas ficam fora desta spec. Veja
[`specs/_inventory.md`](../_inventory.md).

## Regras de negócio

As regras numeradas estão em [`rules.md`](rules.md): dados do anúncio (`R-1` a `R-5`), autoria
(`R-6` a `R-8`), foto (`R-9` a `R-12`), exclusão (`R-13`) e Meus anúncios (`R-14`).

## Requisitos funcionais

### API

| Método e caminho | Acesso | Sucesso | Erros |
|---|---|---|---|
| `POST /api/pecas` | Autenticado | 201, com `Location` e `PecaResponse` | 400, 401, 404 (usuário do token não existe) |
| `PUT /api/pecas/{id}` | Só o vendedor | 200, com `PecaResponse` | 400, 401, 403, 404 |
| `POST /api/pecas/{id}/imagem` | Só o vendedor | 200, com `PecaResponse` | 400, 401, 403, 404, 413, 500 |
| `DELETE /api/pecas/{id}` | Só o vendedor | 204 | 401, 403, 404 |

`PUT` substitui todos os campos de `PecaRequest`. Erros usam `ProblemDetail`.

### Front

- `/anunciar` e `/pecas/[id]/editar` exigem sessão: sem ela, redirecionam para `/login` com `destino`.
- `/anunciar` preenche o WhatsApp com o do perfil. Se o perfil falhar, o campo começa vazio.
- `/pecas/[id]/editar` preenche os campos, mostra a foto atual e oferece trocar a foto e excluir.
- A exclusão pede confirmação em `AlertDialog`.
- Depois de publicar, vai para `/`; de salvar, para `/pecas/{id}`; de excluir, para
  `/perfil?aba=anuncios`.
- Em caso de erro, os campos do formulário são mantidos e um toast explica o problema.
- `/perfil?aba=anuncios` lista os anúncios da pessoa, com link para editar.

## Requisitos não funcionais

- **Dados pessoais:** o anúncio expõe publicamente nome e WhatsApp do vendedor, nunca o email. Esta
  feature grava `usuarios.whatsapp`.
- **Consistência:** cada chamada à API roda em uma transação. Publicar com foto são duas chamadas
  separadas, sem atomicidade entre elas.
- **Timeouts no front:** 30 s para upload e 10 s para as demais chamadas.
- **Testes:** `PecaServiceTest`, `PecaRepositoryTest`, `ArmazenamentoImagemServiceTest` e
  `SegurancaIntegrationTest`. O caminho feliz de `deletar` só é coberto na integração, sem verificar
  a remoção do arquivo, e a gravação do WhatsApp ao editar não tem teste próprio. O front não tem
  testes.

## Critérios de aceite

Descrevem o comportamento atual.

- `AC-1`: `POST /api/pecas` com token e dados válidos responde 201, e o vendedor é o dono do token.
- `AC-2`: sem token, as rotas de escrita respondem 401.
- `AC-3`: anúncio sem WhatsApp responde 400.
- `AC-4`: outro usuário recebe 403 em `PUT`, `DELETE` e `POST .../imagem`, sem alterar nada.
- `AC-5`: id inexistente responde 404 em `PUT`, `DELETE` e `POST .../imagem`.
- `AC-6`: arquivo que não seja JPEG, PNG ou WEBP pelo conteúdo responde 400.
- `AC-7`: enviar nova foto substitui `imagemUrl` e apaga o arquivo anterior.
- `AC-8`: `/anunciar` recusa publicar sem foto ou com categoria fora da lista.
- `AC-9`: se o upload falhar depois de criar o anúncio, o front avisa "Anúncio publicado sem foto".
- `AC-10`: se a foto falhar ao editar, o front avisa que os dados foram salvos e a foto não mudou.
- `AC-11`: quem não é o vendedor vê "Este anúncio não é seu." em `/pecas/[id]/editar`.
- `AC-12`: excluir um anúncio que já não existe aparece como sucesso no front.
- `AC-13`: `/perfil?aba=anuncios` mostra só anúncios cujo `vendedor.id` é o `sub` do token.

## Casos de borda

- **Sessão expirada:** o front apaga os cookies, mantém os campos e oferece "Entrar".
- **Usuário do token removido:** `POST /api/pecas` responde 404, e o front encerra a sessão.
- **Falha no upload com token expirado:** ao criar, vira publicação parcial, não erro de sessão.
- **Edições simultâneas:** não há controle de versão, e a última gravação vale.
- **Foto acima de 5 MB:** o navegador bloqueia antes do envio; a API responde 413.
- **Tipo declarado diferente do conteúdo:** o front confere o tipo informado pelo navegador; a API
  decide pelo conteúdo.
- **Descrição vazia:** o front envia texto vazio, que é gravado assim, não como nulo.
- **Backend fora do ar:** escrita mostra erro de conexão; `/pecas/[id]/editar` e Meus anúncios mostram
  um aviso para tentar de novo.

## Fora do escopo

Vitrine, busca, filtros, detalhe da peça e contato. Entrega das fotos em `GET /api/imagens/...`.
Login, perfil e remoção de conta. Moderação, pausa de anúncio e várias fotos, que não existem.
