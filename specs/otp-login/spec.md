# Login por OTP

Status: reverse-engineered from code on 2026-11-05; `R-3`, `R-8` e `R-11` corrigidas em 2026-11-05

Descreve o que o sistema faz hoje. Comportamento considerado errado está em
[`rules.md`](rules.md#defeitos-observados), não aqui como requisito.

## Objetivo

Uma pessoa com conta entra no PEÇA sem senha: informa o email, recebe um código de 6 dígitos e troca
esse código por um JWT. Os limites de pedidos e o envio do email fazem parte desta spec.

## Contexto

Quem usa é o técnico com conta. Participam `peca-front-end` (`/login`, `lib/api/auth.ts`,
`lib/resultado-auth.ts`, `lib/visitante.ts`) e `peca-backend` (`auth`, `email`), com PostgreSQL e SMTP.
Sem SMTP real, os emails vão para o Mailpit.

Os mesmos endpoints atendem o cadastro. Quando o email não tem conta e o pedido traz `nome`, o fluxo é
o de `otp-signup`, ainda sem spec. A guarda dos cookies no front fica em `web-session`.

## Regras de negócio

As regras numeradas estão em [`rules.md`](rules.md): identidade (`R-1` a `R-3`), código (`R-4` a `R-7`),
limites (`R-8` e `R-9`), email (`R-10` e `R-11`), token (`R-12` a `R-14`) e tela (`R-15` e `R-16`).

## Requisitos funcionais

### API

| Método e caminho | Acesso | Sucesso | Erros |
|---|---|---|---|
| `POST /api/auth/solicitar-codigo` | Público | 200, com `mensagem` e `expiraEm` | 400, 429 com `Retry-After` |
| `POST /api/auth/verificar-codigo` | Público | 200, com `token`, `tipo`, `expiraEm` e `usuario` | 400, 401 |

- `solicitar-codigo` recebe `email` e, opcionalmente, `nome`.
- `verificar-codigo` recebe `email` e `codigo`.
- Não existe endpoint de login com senha.
- Erros usam `ProblemDetail`.

### Front

- `/login` tem duas etapas: email e código.
- Quem já está logado é redirecionado para `/`.
- A tela usa textos neutros, que não afirmam que o código foi enviado.
- O código é enviado sozinho ao completar 6 dígitos, inclusive ao colar.
- Código errado limpa o campo e devolve o foco.
- "Reenviar código" fica travado por 60 s, ou pelo tempo do `Retry-After`.
- "Trocar email" volta à primeira etapa.
- No sucesso, o JWT vai para o cookie httpOnly e a pessoa segue para `destino`, ou para `/`.
- O front repassa o IP do visitante em `X-Forwarded-For` ao pedir o código.

## Requisitos não funcionais

- **Dados pessoais:** email e nome, em `usuarios` e `codigos_verificacao`; o IP fica só em memória. Pedidos
  sem conta ficam em memória por até 1 hora, só como digest do email com salt.
  O email vai como claim no JWT.
- **Consistência:** pedir o código roda em uma transação. O email sai depois do commit, em fila limitada
  fora da requisição; falha no envio não desfaz o código (`R-11`).
  Validar código persiste as tentativas mesmo quando recusa.
- **Concorrência:** a validação bloqueia o código com lock pessimista.
- **Testes:** `AuthServiceTest`, `SegurancaIntegrationTest`, `LimitadorSolicitacoesPorIpTest`,
  `LimitadorPedidosSemContaTest`, `TempoMinimoRespostaTest`, `EmailServiceTest`, `EnvioCodigoOtpListenerTest` e `AsyncConfigTest`. O front não tem testes.

## Critérios de aceite

Descrevem o comportamento atual.

- `AC-1`: conta existente pedindo código sem `nome` recebe 200, e um código válido é gerado e enviado.
- `AC-2`: email sem conta e sem `nome` recebe 200, sem gerar código nem criar usuário.
- `AC-3`: `nome` enviado no login não altera o perfil.
- `AC-4`: email inválido responde 400.
- `AC-5`: código correto responde 200 com JWT e marca o código como usado.
- `AC-6`: código errado, expirado, já usado ou inexistente responde 401 com a mesma mensagem.
- `AC-7`: 5 erros invalidam o código.
- `AC-8`: um novo pedido invalida os códigos anteriores.
- `AC-9`: novo pedido antes de 60 s responde 429 com `Retry-After`, sem gerar código, e o anterior
  continua válido.
- `AC-10`: passar de 5 pedidos por hora por email, ou de 20 por IP, responde 429.
- `AC-11`: falha no envio do email responde 200, registra a falha no log e mantém o código gravado.
- `AC-12`: `JWT_SECRET` com menos de 32 bytes impede a aplicação de iniciar.
- `AC-13`: `POST /api/auth/login` não existe e responde 401.
- `AC-14`: dois pedidos sem `nome` para um email sem conta, em menos de 60 s, respondem 200 e depois 429
  com `Retry-After`, igual a um email com conta.
- `AC-15`: o sexto pedido em uma hora para um email sem conta, respeitado o intervalo, responde 429.
- `AC-16`: um pedido sem `nome` seguido de um pedido com `nome` em menos de 60 s responde 429, com ou sem
  conta, e não gera código nem envia email.
- `AC-17`: a resposta do pedido não espera o SMTP: com o envio travado, ela chega assim mesmo.
- `AC-18`: se a transação do pedido falhar, nenhum email é enviado.
- `AC-19`: pedidos sem conta e sem `nome` não gravam nada no banco e não enviam email.
- `AC-20`: toda resposta do pedido, 200 ou 429, com ou sem conta, leva pelo menos 50 ms.

## Casos de borda

- **Maiúsculas e espaços no email:** o email é normalizado antes de buscar.
- **Código perto de expirar:** vale até antes do instante de expiração, nesse instante já não vale.
- **Validações simultâneas:** o lock impede usar o mesmo código duas vezes.
- **Código com zeros à esquerda:** é aceito, porque o código é texto de 6 dígitos.
- **Conta removida:** os códigos dela somem junto, por `ON DELETE CASCADE`.
- **Sessão ativa:** quem está logado não vê `/login`.
- **Sem conta indo para o cadastro:** quem tenta entrar sem conta e abre "Criar conta" logo depois recebe 429
  e espera até 60 s. É efeito aceito de `R-3`.
- **Reinício da aplicação:** os pedidos sem conta em memória são perdidos, assim como os emails na fila. O
  código gravado continua valendo, e a pessoa pede outro pelo reenvio.
- **Fila de envio cheia:** o pedido responde 200 e a falha vai para o log, como em `R-11`.

## Fora do escopo

Cadastro (`otp-signup`), cookies, `proxy.ts` e menu da conta (`web-session`), proteção de rotas
(`api-access-control`), troca de email e login social.
