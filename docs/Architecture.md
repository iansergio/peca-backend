# Arquitetura — PEÇA

Levantamento feito por engenharia reversa em 2026-11-05. "Observado" vem da leitura do código e das
configurações; os testes não foram executados nesse levantamento. Regras de módulo:
[`ai/module_map.md`](../ai/module_map.md). Convenções: [`ai/conventions.md`](../ai/conventions.md).

## Visão geral

```text
navegador → peca-front-end (servidor Next, :3000) → peca-backend (/api, :8080) → PostgreSQL
                                                                               → disco (fotos)
                                                                               → SMTP ou Mailpit
```

## Observado

### Apps e execução

- `peca-backend`: API REST em Java 25 e Spring Boot 4.1; entrada em `PecaBackendApplication`.
- `peca-front-end`: Next.js 16 com App Router, Cache Components e saída `standalone`.
- O `docker-compose.yml` do backend sobe `postgres`, `mailpit`, `api` e `web`; o front é construído
  a partir do repositório vizinho (`FRONTEND_DIR`).
- O front chama a API só pelo servidor, por `lib/api/cliente.ts`. As fotos passam pela rota
  `/imagens/[pasta]/[arquivo]`, que repassa para `GET /api/imagens/...`.

### Camadas

- Backend em pacotes por feature (`auth`, `peca`, `usuario`, `storage`, `email`), com Controller,
  Service, Repository, Entity e `dto/`. As regras ficam nos Services; as entidades só guardam dados.
- `exception` centraliza os erros em `ProblemDetail`; `config` cuida de CORS e OpenAPI.
- Front com Server Components, Server Actions em `app/**/actions.ts`, acesso à API em `lib/api/` e
  componentes em `components/<dominio>/`.

### Dados

- PostgreSQL 18, Flyway e Hibernate com `ddl-auto=validate`. Existe uma migration:
  `V1__schema_inicial.sql`.
- Tabelas: `usuarios`, `pecas` e `codigos_verificacao`, com UUID e `snake_case`.
- Remover usuário com anúncios falha na chave estrangeira e responde 409.
- Transações ficam nos Services, e `open-in-view` está desligado.

### Integrações

- Email por SMTP no `EmailService`; sem `MAIL_*`, vai para o Mailpit.
- Fotos no disco pelo `ArmazenamentoImagemService`: JPEG, PNG ou WEBP de até 5 MB, com o tipo
  detectado pelo conteúdo do arquivo.
- As integrações são classes concretas, sem interface de porta.

### Autenticação e sessão

- OTP de 6 dígitos válido por 10 minutos, com até 5 tentativas por código. O código mais recente
  invalida os anteriores.
- O usuário só é criado quando o código é validado.
- Limites de pedidos: 60 s entre pedidos, 5 por hora por email e 20 por hora por IP (em memória).
- JWT HS256 válido por 2 horas (padrão), guardado pelo front no cookie httpOnly `peca_sessao`.
- A autorização fica em `SecurityConfig` e nos Services (dono do anúncio e da conta). Não há papéis.
- `proxy.ts` faz uma checagem otimista só para a experiência de uso, sem validar a assinatura.

### Convenções transversais

- Validação por Bean Validation nos DTOs; erros em `ProblemDetail`.
- Configuração por variáveis de ambiente, documentadas em `.env.example` e no README.
- Swagger em todos os controllers. Health por Actuator e `/api/saude` no front.
- Sem camada de i18n nem feature flags.

### Qualidade

- Backend: 87 casos de teste em 13 classes, com JUnit, Mockito e H2.
- Front: sem testes; `npm run lint` e `npm run build`.
- Nenhum repositório tem CI: os workflows do GitHub Actions foram removidos com o deploy na Azure.
- Backend sem linter, formatador ou auditoria de dependências.

## Open points

Nada abaixo foi corrigido. Cada item precisa de decisão e, quando virar mudança, de uma spec.

### Segurança e dados

- **P-1: Enumeração de email pelo 429.** Email sem conta e sem nome nunca grava código, então nunca cai
  no limite por email e responde sempre 200. Conta existente recebe 429 no segundo pedido dentro
  de 60 s, e 502 se o envio falhar. O comentário em `AuthService.solicitarCodigo` afirma o
  contrário. O tempo de resposta também difere, porque só um dos casos envia email.
- **P-2: Limite por IP contornável.** O `docker-compose.yml` publica a porta 8080, e a API aceita
  `X-Forwarded-For` de quem a chamar, como já avisa o `application.properties`. A confirmar: o
  front lê a entrada mais à direita desse header e o Compose não tem proxy reverso, então um
  cliente também pode conseguir forjá-lo pela porta 3000.
- **P-3: Portas abertas na rede local.** PostgreSQL (com padrão `peca`/`peca`), Mailpit e API são
  publicados em todas as interfaces. No Mailpit, os códigos OTP ficam legíveis para quem alcançar
  a porta 8025, o que permite entrar na conta de outra pessoa.
- **P-4: Segredo JWT conhecido.** `docker-compose.yml` e `.env.example` versionam um valor padrão para
  `JWT_SECRET`. Sem um `.env` próprio, quem conhece esse valor pode forjar tokens. É um padrão de
  desenvolvimento, mas não há nada que impeça usá-lo com a máquina exposta.
- **P-5: OTP em texto puro e sem expurgo.** `codigos_verificacao.codigo` não é protegido por hash, e
  nenhuma rotina apaga códigos antigos. Cadastros não concluídos mantêm nome e email sem prazo de
  retenção, o que também é um ponto de LGPD.
- **P-6: Remoção de conta incompleta.** `DELETE /api/usuarios/{id}` não tem tela no front e responde
  409 enquanto a pessoa tiver anúncios. Falta definir o atendimento ao pedido de exclusão da LGPD.
- **P-7: Caminho interno no erro de upload.** `ArmazenamentoException` leva `e.getMessage()` do
  `IOException` para a resposta 500, o que pode revelar caminhos do servidor.
- **P-8: Swagger público.** A UI e `/v3/api-docs` ficam abertos em qualquer ambiente, por decisão ou por
  padrão; falta confirmar.

### Consistência e contrato

- **P-9: Arquivo apagado antes do commit.** `PecaService.deletar` e `enviarImagem` removem o arquivo
  dentro da transação. Se o commit falhar, o registro continua apontando para uma foto que não
  existe mais. Em `enviarImagem`, o arquivo novo também fica órfão.
- **P-10: Listagem sem paginação.** `GET /api/pecas` devolve tudo. O front filtra busca, categoria e
  "Meus anúncios" na própria memória, com a lista inteira, inclusive o WhatsApp de todos.
- **P-11: Categoria sem contrato.** A lista fixa de `components/vitrine/categorias.ts` é a oficial, mas a
  API aceita texto livre. Ver `R-5` e `D-3` em `specs/listing-management/rules.md`. `condicao` existe só
  no tipo do front.
- **P-12: Anúncio sobrescreve o perfil.** Criar ou editar anúncio troca `usuarios.whatsapp`, afetando
  todos os anúncios da pessoa. É intencional: ver `R-4` em `specs/listing-management/rules.md`.

### Qualidade e manutenção

- **P-13: Migrations fora dos testes.** Os testes usam H2 com `create-drop` e Flyway desligado, então um
  erro de migration só aparece ao subir no PostgreSQL. O `application.properties` de teste também
  precisa ser mantido à mão.
- **P-14: Lacunas de ferramentas.** Sem CI, sem testes no front, sem linter nem auditoria no backend.
- **P-15: Limitador em memória.** O limite por IP zera ao reiniciar e vale por instância.
- **P-16: Dependência circular entre pacotes.** `auth` usa `usuario`, e `usuario` e `peca` usam
  `auth.UsuarioAutenticado`.
- **P-17: Documentos desatualizados.** O `ROADMAP.md` do backend ainda lista Supabase e Azure como
  concluídos, embora tenham sido substituídos. Um comentário em `lib/api/anuncios.ts` fala em
  "Storage". O nome aparece como "PECA" e "PEÇA".
- **P-18: Histórico do git.** A varredura por padrões achou só hostnames de Supabase e Azure em versões
  antigas, sem credenciais. Essa varredura não é exaustiva.
