# Fluxos — listing-management

Status: reverse-engineered from code on 2026-11-05

## F-1 — Publicar anúncio (caminho feliz)

1. O técnico abre `/anunciar`, com o WhatsApp do perfil já preenchido.
2. O navegador bloqueia foto acima de 5 MB (`R-10`).
3. A Server Action `publicarAnuncio` confere a sessão e valida o formulário (`R-1` a `R-3`, `R-5`, `R-11`).
4. O front chama `POST /api/pecas`; `PecaService.criar` define o vendedor (`R-6`) e grava o WhatsApp no
   perfil (`R-4`).
5. O front chama `POST /api/pecas/{id}/imagem`; a API valida e grava a foto (`R-9`, `R-10`).
Resultado: toast "Anúncio publicado!", redirecionamento para `/` e anúncio na vitrine (`R-8`).

## F-2 — Upload falha ao publicar

1. Os passos de F-1 vão até o 4.
2. `POST /api/pecas/{id}/imagem` falha ou estoura o tempo limite (`R-12`).
Resultado: toast "Anúncio publicado sem foto" e redirecionamento para `/`; o anúncio fica na vitrine sem
foto (`D-1`).

## F-3 — Editar anúncio

1. O vendedor abre `/pecas/[id]/editar` por Meus anúncios.
2. A página busca a peça e confere se o vendedor é a pessoa logada.
3. A Server Action `salvarAnuncio` valida o formulário, com foto opcional (`R-11`).
4. O front chama `PUT /api/pecas/{id}`; a API confere o vendedor (`R-7`), substitui os dados e grava o
   WhatsApp no perfil (`R-4`).
5. Se houver foto nova, o front chama `POST /api/pecas/{id}/imagem` (`R-9`).
Resultado: toast "Anúncio atualizado!" e redirecionamento para `/pecas/{id}`. Se só a foto falhar, os
dados ficam salvos, a foto antiga continua e o toast avisa (`R-12`).

## F-4 — Excluir anúncio

1. O vendedor clica em "Excluir anúncio" e confirma no `AlertDialog`.
2. A Server Action `removerAnuncio` confere a sessão e chama `DELETE /api/pecas/{id}`.
3. A API confere o vendedor (`R-7`), remove o registro e apaga o arquivo (`R-13`).
Resultado: toast "Anúncio excluído" e redirecionamento para `/perfil?aba=anuncios`.

## F-5 — Outra pessoa tenta alterar o anúncio

1. Um usuário abre `/pecas/[id]/editar` de um anúncio que não é dele.
2. O front mostra "Este anúncio não é seu." e não exibe o formulário.
3. Chamando a API diretamente, ele recebe 403 (`R-7`).
Resultado: nada muda.

## F-6 — Sessão expira com o formulário aberto

1. A pessoa envia o formulário com o token expirado ou recusado.
2. A Server Action devolve `precisaLogin`, e os cookies são apagados quando a API recusa o token.
Resultado: toast com a ação "Entrar", campos mantidos e nada gravado.

## F-7 — Ver Meus anúncios

1. O técnico abre `/perfil?aba=anuncios`.
2. O servidor do front busca `GET /api/pecas` e filtra pelo `sub` do token (`R-14`).
Resultado: lista com link para editar, estado vazio com "Anunciar peça" ou aviso de falha.
