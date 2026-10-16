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
- [x] Criar Entidade e DTOs de Usuário (Nome, Email, Senha, WhatsApp).
- [x] Criar CRUD de Usuários e configuração base de segurança. (Senha com hash BCrypt; JWT na Fase 5.)

## Fase 3: Domínio de Anúncios (Peças)
- [x] Criar Entidade e DTOs de Peça (Título, Descrição, Preço, Categoria, Modelo de Celular, ID do Vendedor).
- [x] Criar endpoints de criação, edição e exclusão de anúncios. (Autoria via JWT na Fase 5.)
- [x] Criar endpoint público de listagem de peças com filtros (por modelo e preço).

## Fase 4: Imagens
- [x] Integrar API do Supabase Storage para upload de imagens das peças.
- [x] Salvar a URL pública da imagem na Entidade da Peça.

## Fase 5: Autenticação JWT
- [-] Criar endpoint de login (`POST /api/auth/login`) que emite token JWT.
- [-] Proteger endpoints de escrita com JWT (cadastro de usuário e leitura de peças continuam públicos).
- [-] Usar o usuário do token como vendedor e restringir edição/exclusão/upload ao próprio dono.
