# Roadmap: Backend API

## Regra de Preenchimento
- [ ] Pendente
- [-] Implementado, testes passando, mas aguardando teste humano no Swagger/Postman[cite: 2].
- [x] Testado pelo humano e aprovado[cite: 2].

## Fase 1: Setup e Infraestrutura
- [x] Inicializar projeto Spring Boot 4.1 com Java 25 LTS.
- [x] Configurar conexão com o banco de dados Supabase (PostgreSQL).
- [x] Configurar o Swagger/OpenAPI para documentação (acessível via `/swagger-ui.html`).

## Fase 2: Domínio de Usuários
- [-] Criar Entidade e DTOs de Usuário (Nome, Email, WhatsApp). Email é o identificador único e obrigatório.
- [-] Criar endpoints de perfil de Usuário (listar, buscar, editar e remover a própria conta). O cadastro é feito apenas via OTP (Fase 5); não existe mais `POST /api/usuarios`.
- [-] Remover o campo Senha (e o hash BCrypt) da entidade, DTOs e CRUD de Usuário.

## Fase 3: Domínio de Anúncios (Peças)
- [x] Criar Entidade e DTOs de Peça (Título, Descrição, Preço, Categoria, Modelo de Celular, ID do Vendedor).
- [x] Criar endpoints de criação, edição e exclusão de anúncios. (Autoria via JWT na Fase 5.)
- [x] Criar endpoint público de listagem de peças com filtros (por modelo e preço).

## Fase 4: Imagens
- [x] Integrar API do Supabase Storage para upload de imagens das peças.
- [x] Salvar a URL pública da imagem na Entidade da Peça.

## Fase 5: Autenticação (Passwordless OTP + JWT)
- [-] Criar a entidade `CodigoVerificacao` (código OTP de 6 dígitos vinculado ao email, com expiração).
- [-] Criar endpoint para solicitar código OTP por email (`POST /api/auth/solicitar-codigo`, cria o usuário se ele ainda não existir).
- [-] Enviar o código OTP por email real via SMTP (`spring-boot-starter-mail` + `EmailService`), substituindo a simulação no console. Credenciais via `MAIL_*` no `.env`.
- [-] Criar endpoint para validar o código OTP e emitir o token JWT (`POST /api/auth/verificar-codigo`).
- [-] Remover o endpoint de login com senha (`POST /api/auth/login`).
- [-] Proteger endpoints de escrita com JWT (apenas os endpoints de OTP e a leitura de peças são públicos).
- [-] Usar o usuário do token como vendedor e restringir edição/exclusão/upload ao próprio dono.
