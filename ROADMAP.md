# Roadmap: Backend API

## Regra de Preenchimento
- [ ] Pendente
- [-] Implementado, testes passando, mas aguardando teste humano no Swagger/Postman[cite: 2].
- [x] Testado pelo humano e aprovado[cite: 2].

## Fase 1: Setup e Infraestrutura
- [x] Inicializar projeto Spring Boot 4.1 com Java 25 LTS.
- [x] Configurar conexão com o banco de dados Supabase (PostgreSQL).
- [x] Configurar o Swagger/OpenAPI para documentação (acessível via `/swagger-ui.html`).
- [-] Migrar o banco para Azure SQL Database com migrations Flyway.

## Fase 2: Domínio de Usuários
- [x] Criar Entidade e DTOs de Usuário (Nome, Email, WhatsApp). Email é o identificador único e obrigatório.
- [-] Criar endpoints de perfil de Usuário restritos à própria conta: `GET /api/usuarios/me`, buscar (`GET /api/usuarios/{id}`, 403 para outro id), editar e remover. Não existe listagem de usuários; dados públicos do vendedor saem só em `PecaResponse.vendedor` (sem email). O cadastro é feito apenas via OTP (Fase 5); não existe mais `POST /api/usuarios`.
- [x] Remover o campo Senha (e o hash BCrypt) da entidade, DTOs e CRUD de Usuário.

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
- [-] Exigir o nome do usuário ao solicitar o código OTP (`nome` obrigatório em `POST /api/auth/solicitar-codigo`); o nome é usado no cadastro de um usuário novo e não sobrescreve o de um usuário existente.
- [x] Enviar o código OTP por email real via SMTP (`spring-boot-starter-mail` + `EmailService`), substituindo a simulação no console. Credenciais via `MAIL_*` no `.env`.
- [x] Criar endpoint para validar o código OTP e emitir o token JWT (`POST /api/auth/verificar-codigo`).
- [x] Remover o endpoint de login com senha (`POST /api/auth/login`).
- [x] Proteger endpoints de escrita com JWT (apenas os endpoints de OTP e a leitura de peças são públicos).
- [x] Usar o usuário do token como vendedor e restringir edição/exclusão/upload ao próprio dono.
- [-] Limitar `POST /api/auth/solicitar-codigo` (60 s entre pedidos e 5 por hora por email, 20 por hora por IP; configurável em `app.otp.*`), respondendo 429 com `Retry-After`, e criar o usuário só ao validar o código (substitui o cadastro no momento do pedido).
