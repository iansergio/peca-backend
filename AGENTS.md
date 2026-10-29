# Regras do Agente: Backend E-commerce de Peças

## Stack Tecnológica Obrigatória
- Linguagem: Java 25 LTS.
- Framework: Spring Boot 4.1.
- Banco de Dados: PostgreSQL em container (migrations versionadas com Flyway).
- Storage: disco local / volume do container (para fotos).
- Ambiente: tudo roda numa máquina só via `docker compose up --build` (PostgreSQL, Mailpit, API e front-end). Não adicione dependências de serviços externos.
- Documentação da API: Swagger/OpenAPI.

## Regras de Comportamento e Código
1. Siga os padrões do Spring Boot: Controller, Service, Repository, Entity.
2. Utilize DTOs (Data Transfer Objects) para entrada e saída de dados. Nunca exponha as Entidades JPA diretamente nos Controllers.
3. Todas as rotas da API devem ser documentadas via anotações do Swagger/OpenAPI.
4. Senhas e variáveis sensíveis (como credenciais do banco e o segredo do JWT) devem vir de variáveis de ambiente (`application.properties` via `System.getenv`).
5. Proteja os endpoints de escrita (criação de anúncios, edição de perfis) com autenticação JWT.
6. O sistema de usuários **não utiliza senhas**. O email é o único identificador do usuário e a autenticação é feita exclusivamente via Passwordless OTP (código de 6 dígitos enviado por email), que, ao ser validado, emite o token JWT. Não crie campos, endpoints ou fluxos baseados em senha.

## Fluxo de Trabalho (Obrigatório)
1. Trabalhe em branches isoladas (ex: `feat/api-anuncios`, `fix/conexao-banco`).
2. Utilize Conventional Commits em todas as mensagens de commit[cite: 2].
3. Escreva testes unitários básicos (JUnit/Mockito) para lógicas críticas nos Services.
4. Certifique-se de que o projeto compila sem erros (`./mvnw clean package`).
5. Atualize o `ROADMAP.md` seguindo a regra de honestidade[cite: 2].
6. Abra o Pull Request via GitHub CLI (`gh`) usando o template `.github/pull_request_template.md`[cite: 2].
