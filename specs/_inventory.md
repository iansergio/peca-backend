# Inventário de funcionalidades — PEÇA

Funcionalidades encontradas por engenharia reversa em 2026-11-05. Esta lista é o backlog do
`/peca.reverse`: cada item ganha uma spec em `/specs/<slug>/` antes de ser alterado. Nenhum item
tem spec ainda. Os números `P-n` apontam para os "Open points" de
[`docs/Architecture.md`](../docs/Architecture.md).

"Testes" considera os dois apps: `parcial` indica teste no backend e nenhum no front.

| Funcionalidade | Slug | Área | Pontos de entrada | Testes | Spec | Notas |
|---|---|---|---|---|---|---|
| Login por OTP | `otp-login` | Autenticação | `POST /api/auth/solicitar-codigo`, `POST /api/auth/verificar-codigo`; `/login` | parcial | none | P-1, P-5 |
| Cadastro por OTP | `otp-signup` | Autenticação | Mesmos endpoints, com `nome`; `/registo` | parcial | none | Usuário criado só ao validar o código; P-5 |
| Limites de pedidos de OTP | `otp-rate-limit` | Autenticação | `AuthService`, `LimitadorSolicitacoesPorIp`, `lib/visitante.ts` | parcial | none | P-1, P-2, P-15 |
| Envio do email de OTP | `otp-email` | Autenticação | `EmailService` | yes | none | SMTP real ou Mailpit; P-3 |
| Sessão no front | `web-session` | Autenticação | `lib/sessao.ts`, `lib/jwt.ts`, `proxy.ts`, menu da conta, `sair` | no | none | Header pode mostrar a sessão até 5 min após expirar |
| Proteção das rotas da API | `api-access-control` | Autenticação | `SecurityConfig`, `UsuarioAutenticado` | yes | none | P-8 |
| Perfil do usuário | `user-profile` | Usuários | `GET /api/usuarios/me`, `GET` e `PUT /api/usuarios/{id}`; `/perfil` | parcial | none | Email não editável |
| Remoção de conta | `account-deletion` | Usuários | `DELETE /api/usuarios/{id}` | yes | none | Sem tela no front; P-6 |
| Criar anúncio | `listing-create` | Anúncios | `POST /api/pecas`, `POST /api/pecas/{id}/imagem`; `/anunciar` | parcial | none | Foto obrigatória só no front; P-11, P-12 |
| Editar anúncio | `listing-edit` | Anúncios | `PUT /api/pecas/{id}`, `POST /api/pecas/{id}/imagem`; `/pecas/[id]/editar` | parcial | none | Só o vendedor; P-9, P-12 |
| Excluir anúncio | `listing-delete` | Anúncios | `DELETE /api/pecas/{id}`; `/pecas/[id]/editar` | parcial | none | Só o vendedor; P-9 |
| Vitrine e listagem | `listing-browse` | Vitrine | `GET /api/pecas`; `/`, `/pecas` | parcial | none | Filtros por modelo e preço na API; P-10 |
| Busca e filtro por categoria | `listing-search` | Vitrine | `lib/filtro-pecas.ts`, `components/vitrine/categorias.ts`; `/pecas?q=&categoria=` | no | none | Filtra no front; P-10, P-11 |
| Atalhos de categorias | `category-shortcuts` | Vitrine | `categoriasRapidas`, `components/layout/categorias-rapidas.tsx` | no | `specs/category-shortcuts` | Filtra no front; P-10, P-11 |
| Detalhe da peça | `listing-detail` | Vitrine | `GET /api/pecas/{id}`; `/pecas/[id]` | parcial | none | |
| Contato pelo WhatsApp | `seller-contact` | Vitrine | `VendedorResponse`, `lib/whatsapp.ts`, `components/peca/contato-anunciante.tsx` | parcial | none | `vendedor.whatsapp` pode vir nulo |
| Meus anúncios | `my-listings` | Anúncios | `components/perfil/meus-anuncios.tsx`; `/perfil?aba=anuncios` | no | none | Filtra pelo `vendedor.id` no front; P-10 |
| Fotos dos anúncios | `listing-photos` | Armazenamento | `ArmazenamentoImagemService`, `GET /api/imagens/{pasta}/{arquivo}`; `/imagens/[pasta]/[arquivo]` | parcial | none | Uma foto por peça; P-7, P-9 |
| Documentação da API | `api-docs` | Plataforma | `/swagger-ui.html`, `/v3/api-docs` | yes | none | P-8 |
| CORS | `cors` | Plataforma | `CorsConfig`, `CORS_ALLOWED_ORIGINS` | yes | none | Opcional, o front não depende dele |
| Health checks | `health-checks` | Plataforma | `/actuator/health/**`; `/api/saude` | parcial | none | Readiness inclui o banco |
| Ambiente local | `local-environment` | Plataforma | `docker-compose.yml`, Dockerfiles | no | none | P-3, P-4 |
