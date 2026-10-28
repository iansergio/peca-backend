# Roadmap: Backend API

## Regra de Preenchimento
- [ ] Pendente
- [-] Implementado, testes passando, mas aguardando teste humano no Swagger/Postman.
- [x] Testado pelo humano e aprovado.

## Fase 1: Setup e Infraestrutura
- [x] Inicializar projeto Spring Boot 4.1 com Java 25 LTS.
- [x] Configurar conexão com o banco de dados Supabase (PostgreSQL).
- [x] Configurar o Swagger/OpenAPI para documentação (acessível via `/swagger-ui.html`).
- [-] Migrar o banco para Azure SQL Database com migrations Flyway.

## Fase 2: Domínio de Usuários
- [x] Criar Entidade e DTOs de Usuário (Nome, Email, WhatsApp). Email é o identificador único e obrigatório.
- [-] Criar endpoints de perfil de Usuário restritos à própria conta: `GET /api/usuarios/me`, buscar (`GET /api/usuarios/{id}`, 403 para outro id), editar e remover. Não existe listagem de usuários; dados públicos do vendedor saem só em `PecaResponse.vendedor` (sem email). O cadastro é feito apenas via OTP (Fase 5); não existe mais `POST /api/usuarios`.
- [x] Remover o campo Senha (e o hash BCrypt) da entidade, DTOs e CRUD de Usuário.
- [-] Edição de perfil não altera e-mail (`PUT /api/usuarios/{id}` aceita só nome e WhatsApp; o e-mail é a identidade confirmada por OTP).
- [ ] Troca de e-mail com confirmação por OTP no novo endereço.

## Fase 3: Domínio de Anúncios (Peças)
- [x] Criar Entidade e DTOs de Peça (Título, Descrição, Preço, Categoria, Modelo de Celular, ID do Vendedor).
- [x] Criar endpoints de criação, edição e exclusão de anúncios. (Autoria via JWT na Fase 5.)
- [x] Criar endpoint público de listagem de peças com filtros (por modelo e preço).
- [-] Exigir número de contato (`whatsapp`, 10 a 13 dígitos) ao criar/editar anúncio; o número é salvo no perfil do vendedor e exibido em `vendedor.whatsapp`.

## Fase 4: Imagens
- [x] Integrar API do Supabase Storage para upload de imagens das peças.
- [x] Salvar a URL pública da imagem na Entidade da Peça.

## Fase 5: Autenticação (Passwordless OTP + JWT)
- [x] Criar a entidade `CodigoVerificacao` (código OTP de 6 dígitos vinculado ao email, com expiração).
- [x] Criar endpoint para solicitar código OTP por email (`POST /api/auth/solicitar-codigo`, cria o usuário se ele ainda não existir).
- [-] Nome do usuário ao solicitar o código OTP: obrigatório só no cadastro (email sem conta). No login de um usuário existente basta o email; email sem conta e sem nome responde 200 sem gerar nem enviar código, para não revelar quais emails estão cadastrados. O nome não sobrescreve o de um usuário existente. O `peca-front-end` já removeu o contorno que tratava o antigo 400 como envio neutro.
- [x] Enviar o código OTP por email real via SMTP (`spring-boot-starter-mail` + `EmailService`), substituindo a simulação no console. Credenciais via `MAIL_*` no `.env`.
- [x] Criar endpoint para validar o código OTP e emitir o token JWT (`POST /api/auth/verificar-codigo`).
- [x] Remover o endpoint de login com senha (`POST /api/auth/login`).
- [x] Proteger endpoints de escrita com JWT (apenas os endpoints de OTP e a leitura de peças são públicos).
- [x] Usar o usuário do token como vendedor e restringir edição/exclusão/upload ao próprio dono.
- [-] Limitar `POST /api/auth/solicitar-codigo` (60 s entre pedidos e 5 por hora por email, 20 por hora por IP; configurável em `app.otp.*`), respondendo 429 com `Retry-After`, e criar o usuário só ao validar o código (substitui o cadastro no momento do pedido).

## Fase 6: Deploy e integração com o front
- [-] CORS configurável por ambiente (`CORS_ALLOWED_ORIGINS`), com preflight sem token e `Retry-After`/`Location` expostos. Opcional: o front Next.js chama a API pelo servidor e não depende de CORS.
- [-] Health check via Actuator (`/actuator/health` com probes de liveness/readiness; readiness inclui o banco), usado no Health check do App Service.
- [-] Dockerfile multi-stage e `docker-compose.yml` só para desenvolvimento local (produção roda o JAR no App Service).
- [-] Deploy contínuo no Azure App Service (Java SE 25, app `peca-backend`): a cada push na `main`, `main_peca-backend.yml` roda `./mvnw -B clean package` com cache do Maven e publica o JAR (um deploy por vez). Em todo PR para a `main`, `verificacao.yml` roda testes e build sem deploy.
- [-] Documentação do deploy (`docs/deploy-azure.md`): App Settings, porta definida pela plataforma (`PORT`), health check em `/actuator/health`, IP do cliente e rollback.
- [ ] Configurar as App Settings e o Health check no portal e validar o deploy no App Service contra o Supabase real.
