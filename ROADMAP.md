# Roadmap: Backend API

## Regra de Preenchimento
- [ ] Pendente
- [-] Implementado, testes passando, mas aguardando teste humano no Swagger/Postman[cite: 2].
- [x] Testado pelo humano e aprovado[cite: 2].

## Fase 1: Setup e Infraestrutura
- [ ] Inicializar projeto Spring Boot 4.1 com Java 25 LTS.
- [ ] Configurar conexão com o banco de dados Supabase (PostgreSQL).
- [ ] Configurar o Swagger/OpenAPI para documentação (acessível via `/swagger-ui.html`).

## Fase 2: Domínio de Usuários
- [ ] Criar Entidade e DTOs de Usuário (Nome, Email, Senha, WhatsApp).
- [ ] Criar CRUD de Usuários e configuração base de segurança.

## Fase 3: Domínio de Anúncios (Peças)
- [ ] Criar Entidade e DTOs de Peça (Título, Descrição, Preço, Categoria, Modelo de Celular, ID do Vendedor).
- [ ] Criar endpoints de criação, edição e exclusão de anúncios.
- [ ] Criar endpoint público de listagem de peças com filtros (por modelo e preço).

## Fase 4: Imagens
- [ ] Integrar API do Supabase Storage para upload de imagens das peças.
- [ ] Salvar a URL pública da imagem na Entidade da Peça.
