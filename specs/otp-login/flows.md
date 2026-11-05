# Fluxos — otp-login

Status: reverse-engineered from code on 2026-11-05

## F-1 — Entrar com o código (caminho feliz)

1. A pessoa informa o email em `/login`.
2. A Server Action `enviarCodigo` chama `POST /api/auth/solicitar-codigo` sem `nome`, com o IP do
   visitante (`R-2`).
3. `AuthService` normaliza o email (`R-1`), aplica os limites (`R-8`), invalida os códigos anteriores
   (`R-5`) e grava o novo (`R-4`).
4. Depois do commit, a API responde 200 e envia o email fora da requisição (`R-10`, `R-11`).
5. A pessoa digita o código, que é enviado ao completar os 6 dígitos.
6. A Server Action `confirmarCodigo` chama `POST /api/auth/verificar-codigo`.
7. A API confere o código, marca como usado (`R-6`) e emite o JWT (`R-12`).
Resultado: o front grava os cookies e redireciona para `destino` ou `/`.

## F-2 — Email sem conta

1. A pessoa informa um email sem conta em `/login`.
2. A API aplica os mesmos limites de F-1, conta o pedido em memória e responde 200 sem gerar código nem
   enviar email (`R-3`, `R-8`).
Resultado: a tela mostra a etapa do código, com o texto neutro e o link "Criar conta" (`R-15`). Quem seguir
para o cadastro em menos de 60 s recebe 429 e espera (`R-9`).

## F-3 — Código errado

1. A pessoa digita um código errado.
2. A API soma uma tentativa, grava e responde 401 (`R-6`, `R-7`).
3. Na quinta tentativa errada, o código é invalidado.
Resultado: o front limpa o campo, mostra o erro e devolve o foco. Depois de invalidado, até o código
correto recebe 401.

## F-4 — Reenviar o código

1. O botão "Reenviar código" libera depois de 60 s (`R-16`).
2. A pessoa reenvia, e o fluxo repete os passos 2 a 4 de F-1.
Resultado: chega um código novo, e o anterior deixa de valer (`R-5`).

## F-5 — Limite excedido

1. O pedido excede um limite (`R-8`).
2. A API responde 429 com `Retry-After` (`R-9`).
Resultado: a tela mostra "Muitos pedidos de código" e trava o reenvio pelo tempo recebido.

## F-6 — Falha no envio do email

1. A API já respondeu 200, e o SMTP falha ao enviar o email.
2. A falha vai para o log, e o código continua gravado (`R-11`).
Resultado: o email não chega. A pessoa segue na etapa do código e usa "Reenviar código" depois de 60 s.
