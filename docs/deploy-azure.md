# Deploy no Azure App Service

A API roda no Azure App Service com a pilha **Java SE (Java 25), Linux**, como um JAR executável. O app se chama `peca-backend`. O banco é o PostgreSQL do Supabase e as imagens ficam no Supabase Storage.

## Como o deploy acontece

| Workflow | Quando roda | O que faz |
|---|---|---|
| `.github/workflows/verificacao.yml` | Todo PR para a `main` | `./mvnw -B clean package` (testes + build), sem deploy |
| `.github/workflows/main_peca-backend.yml` | Push na `main` ou manual | Mesmo build, depois publica o JAR no App Service com `azure/webapps-deploy` (login por OIDC, secrets `AZUREAPPSERVICE_*`) |

O deploy tem `concurrency`: se dois pushes chegarem juntos, o segundo espera o primeiro terminar.

O `azure/webapps-deploy` renomeia o JAR para `/home/site/wwwroot/app.jar`, e o App Service o inicia com `java -jar`. Não é preciso configurar Startup Command.

Na primeira subida (e a cada versão com migration nova), o Flyway aplica as migrations de `src/main/resources/db/migration` antes de a API aceitar requisições.

## Porta

**Não configure `PORT`, `SERVER_PORT` nem `WEBSITES_PORT`.** O contêiner Java do App Service Linux define a variável de ambiente `PORT` (a documentação da Microsoft manda o app escutar nela) e encaminha o tráfego para essa porta. O `application.properties` já usa `server.port=${PORT:8080}`; o `8080` só vale fora do Azure.

Referências: [Quickstart Java SE](https://learn.microsoft.com/azure/app-service/quickstart-java?pivots=java-javase) (seção que usa `${PORT}` no contêiner Java Linux) e [Configurar apps Java](https://learn.microsoft.com/azure/app-service/configure-language-java-deploy-run).

## App Settings

Portal do Azure > App Service `peca-backend` > **Settings > Environment variables > App settings**. Salvar reinicia o app.

| Nome | Obrigatória | Valor |
|---|---|---|
| `DB_URL` | sim | `jdbc:postgresql://aws-0-<regiao>.pooler.supabase.com:5432/postgres?sslmode=require` (pooler do Supabase em modo sessão; Supabase > Connect > Session pooler) |
| `DB_USERNAME` | sim | `postgres.<ref-do-projeto>` |
| `DB_PASSWORD` | sim | Senha do banco (Supabase > Project Settings > Database) |
| `JWT_SECRET` | sim | Segredo HS256 com 32+ caracteres (`openssl rand -base64 48`) |
| `JWT_EXPIRACAO` | não | Validade do token em ISO-8601. Padrão `PT2H` |
| `MAIL_USERNAME` | sim | Conta SMTP que envia o código OTP |
| `MAIL_PASSWORD` | sim | Senha da conta SMTP (no Gmail, uma senha de app) |
| `MAIL_HOST` / `MAIL_PORT` | não | Padrão `smtp.gmail.com` / `587` |
| `MAIL_FROM` | não | Remetente exibido. Padrão `MAIL_USERNAME` |
| `SUPABASE_URL` | sim | `https://<ref-do-projeto>.supabase.co` (só a raiz) |
| `SUPABASE_SERVICE_KEY` | sim | Secret key / service_role do Supabase. Nunca exponha no front |
| `SUPABASE_STORAGE_BUCKET` | não | Bucket público das imagens. Padrão `pecas` |
| `CORS_ALLOWED_ORIGINS` | não | Só se algum cliente chamar a API pelo navegador. O front Next.js chama pelo servidor e não precisa |
| `JAVA_OPTS` | não | Opções extras da JVM. O App Service já limita o heap a 70% da memória do plano |

> A conexão direta `db.<ref>.supabase.co` só tem IPv6 e não funciona no App Service, que sai por IPv4. Use o pooler. Evite o modo transação (porta 6543): ele não aceita os prepared statements que o driver JDBC usa por padrão.

## Health check

Portal > App Service > **Monitoring > Health check** > habilitar com o caminho **`/actuator/health`**.

- O endpoint é público, não mostra detalhes e responde `503` quando o banco está inacessível (o grupo de saúde inclui o banco).
- Com mais de uma instância, o App Service tira de rotação a instância que falhar no health check. Com uma instância só, ele serve para alertas e reinício automático.
- Probes separadas, se precisar: `/actuator/health/liveness` (só a aplicação) e `/actuator/health/readiness` (inclui o banco).

## IP do cliente e limite de OTP

O limite de `POST /api/auth/solicitar-codigo` por IP usa o `X-Forwarded-For` (`server.forward-headers-strategy=framework`), e o Spring usa a primeira entrada (a mais à esquerda) do header:

- O front chama a API pelo servidor e manda o IP do visitante em `X-Forwarded-For`. O front-end do App Service acrescenta o IP de quem conectou (o front) no fim, então a primeira entrada continua sendo o visitante.
- Como a API é pública, quem chamar a API direto pode forjar essa primeira entrada e escapar do limite por IP. Os limites por email (60 s e 5 por hora) continuam valendo.

## Verificar um deploy

1. Actions > `Build and deploy JAR app to Azure Web App - peca-backend`: os jobs `build` e `deploy` verdes.
2. `curl https://<app>.azurewebsites.net/actuator/health` responde `{"status":"UP"}`.
3. Swagger em `https://<app>.azurewebsites.net/swagger-ui.html`.
4. Logs: Portal > App Service > **Monitoring > Log stream**, ou `az webapp log tail -g <grupo> -n peca-backend`.

## Rollback

Reexecute o workflow de um commit anterior: Actions > workflow de deploy > abra a execução do commit bom > **Re-run all jobs**. Ou faça `git revert` na `main` e deixe o deploy automático publicar.

As migrations do Flyway não são desfeitas no rollback. Uma migration que quebre a versão anterior precisa de uma nova migration para corrigir.
