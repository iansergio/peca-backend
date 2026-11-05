# Regras de negócio — otp-login

Status: reverse-engineered from code on 2026-11-05

## Identidade

### R-1 — Email identifica a pessoa (observed)

O email é o único identificador e é normalizado sem espaços nas pontas e em minúsculas. Não há senha.

### R-2 — Login por email (observed)

Conta existente pede o código só com o email. Um `nome` enviado é ignorado e não altera o perfil.

### R-3 — Não revelar se a conta existe (confirmed by the user on 2026-11-05)

O pedido de código não pode revelar se o email tem conta. Hoje, email sem conta e sem `nome` responde
200 igual a um envio real, sem gerar código. A regra ainda não vale em todos os casos: ver `D-1`.

## Código

### R-4 — Formato e validade (observed)

O código tem 6 dígitos, vem de `SecureRandom` e vale por 10 minutos.

### R-5 — Só o código mais recente vale (observed)

Um novo código invalida os anteriores não usados do mesmo email.

### R-6 — Uso único e tentativas (observed)

Cada código serve uma vez. Com 5 erros, ele é invalidado. A comparação tem tempo constante.

### R-7 — Erro único na validação (observed)

Código errado, expirado, usado, bloqueado ou inexistente responde 401 com a mesma mensagem.

## Limites

### R-8 — Limites de pedidos (observed)

Por padrão: 60 s entre pedidos e 5 por hora para o mesmo email, e 20 por hora por IP, configuráveis em
`app.otp.*`. O limite por IP é checado primeiro, em memória. Os limites por email contam os códigos
gravados na última hora.

### R-9 — Resposta ao exceder (observed)

Exceder um limite responde 429 com `Retry-After` em segundos, sem gerar código. Pedido recusado pelo
limite por IP não conta nesse limite. A validação do código não tem limite por pedido: é limitada pelas
tentativas de `R-6`.

## Email

### R-10 — Conteúdo (observed)

O email tem o assunto "Seu código de acesso - PEÇA", o código e a validade. O remetente vem de
`MAIL_FROM` ou `MAIL_USERNAME`.

### R-11 — Falha no envio (observed)

Se o envio falhar, a API responde 502 e o código é desfeito.

## Token

### R-12 — Conteúdo do JWT (observed)

O token é HS256, com `iss` `peca-backend`, `sub` igual ao id do usuário, `iat`, `exp`, `email` e `nome`.

### R-13 — Validade (observed)

O token vale 2 horas por padrão (`JWT_EXPIRACAO`). Não há renovação. O segredo precisa ter pelo menos
32 bytes.

### R-14 — Sem revogação (observed)

A API não guarda sessões. Um token vale até expirar. Esse comportamento é o defeito `D-3`.

## Tela

### R-15 — Textos neutros (observed)

`/login` diz "Se existir uma conta..." e mostra a etapa do código mesmo sem conta. Um 401 ou 400 ao
validar vira a mensagem "Código inválido ou expirado".

### R-16 — Reenvio (observed)

O reenvio fica travado por 60 s no front, valor fixo, ou pelo `Retry-After` recebido.

## Questões em aberto

- `Q-1`: por quanto tempo códigos, emails e nomes de cadastros não concluídos podem ficar guardados?
- `Q-2`: sem renovação, 2 horas de sessão é o prazo desejado?

## Defeitos observados

Comportamento registrado, não corrigido. Decida a correção antes de usar `/peca.fix`: `D-1`, `D-2` e
`D-3` mudam regras críticas e pedem `/peca.go`.

- `D-1`: **enumeração de contas** (classificado como defeito pelo usuário em 2026-11-05). Email sem conta
  e sem `nome` nunca grava código, então nunca cai no limite por email. Dois pedidos seguidos dão 429
  se a conta existe e sempre 200 se não existe. Conta existente também pode receber 502, e o tempo de
  resposta difere porque só um caso envia email. O comentário em `AuthService.solicitarCodigo` afirma
  o contrário. Ver P-1 em `docs/Architecture.md`.
- `D-2`: **bloqueio de login de terceiros** (classificado como defeito pelo usuário em 2026-11-05).
  Qualquer pessoa pode fazer 5 pedidos por hora para o email de outra, que fica em 429 por até 1 hora e
  recebe os emails. Cinco códigos errados também invalidam o código ativo dela.
- `D-3`: **sair não invalida o token** (classificado como defeito pelo usuário em 2026-11-05). `sair` só
  apaga os cookies. Um token copiado continua aceito até expirar, mesmo depois de remover a conta.
- `D-4`: **limite por IP forjável.** A API usa o `X-Forwarded-For` de quem chama, e a porta 8080 fica
  publicada. Pela porta 3000, o Next preserva o header enviado pelo cliente (`??=` em `base-server`), e
  `ipDoVisitante` usa a entrada mais à direita; sem proxy reverso, ela vem do cliente. Conferido no
  código, não em execução. Ver P-2.
- `D-5`: **OTP em texto puro e sem expurgo.** `codigos_verificacao.codigo` não tem hash, e nenhuma rotina
  apaga códigos antigos. Ver P-5 e `Q-1`.
- `D-6`: **`destinoSeguro` aceita tab e quebra de linha.** `/<TAB>/evil.example` passa pelo filtro, e o
  parser de URL resolve isso para `http://evil.example/`. Conferido no filtro e no parser. Falta testar
  o redirecionamento real no navegador. Se ele se confirmar, é um redirecionamento aberto depois do
  login.
- `D-7`: **códigos expostos no ambiente local.** O Mailpit na porta 8025 mostra os códigos a quem alcançar
  a porta. Ver P-3. O `JWT_SECRET` padrão versionado permite forjar tokens. Ver P-4.
- `D-8`: **corridas no pedido de código.** Sem lock, pedidos simultâneos podem passar juntos pelo limite por
  email. O SMTP roda dentro da transação, segurando a conexão do banco até o timeout, e uma falha no
  commit depois do envio deixa um código que não funciona.
- `D-9`: **limite por IP consome pedidos recusados.** Um pedido recusado pelo limite por email, ou com falha
  no SMTP, gasta o limite por IP, que zera ao reiniciar. Ver P-15.
- `D-10`: **valores repetidos.** Os 10 minutos estão em `AuthService`, `EmailService`, no Swagger e na tela.
  Os 60 s estão no front e no Swagger, embora sejam configuráveis na API. A descrição do 200 no Swagger
  diz "Codigo gerado e enviado" mesmo quando nada é enviado.
