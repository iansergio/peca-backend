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
- [x] Criar CRUD de Usuários e configuração base de segurança. (Senha com hash BCrypt; autenticação JWT dos endpoints de escrita ainda não implementada.)

## Fase 3: Domínio de Anúncios (Peças)
- [x] Criar Entidade e DTOs de Peça (Título, Descrição, Preço, Categoria, Modelo de Celular, ID do Vendedor).
- [x] Criar endpoints de criação, edição e exclusão de anúncios. (Vendedor identificado pelo `vendedorId` no corpo; sem JWT ainda, então não há proteção real de autoria.)
- [x] Criar endpoint público de listagem de peças com filtros (por modelo e preço).

## Fase 4: Imagens
- [x] Integrar API do Supabase Storage para upload de imagens das peças.
- [x] Salvar a URL pública da imagem na Entidade da Peça.
