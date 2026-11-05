# Tarefas — otp-login (correção de D-1)

Checklist de implementação gerado a partir de `spec.md`, `rules.md` e `flows.md`. Cada tarefa indica
o app/módulo/camada e as regras/critérios que implementa, e é marcada quando concluída.

## Contrato

| Interface | Método + caminho / evento | Requisição | Resposta | Erros |
|---|---|---|---|---|
| `peca-backend` → `peca-front-end` | `POST /api/auth/solicitar-codigo` | Sem mudança: `email` e `nome` opcional | Sem mudança: 200 com `mensagem` e `expiraEm` | 400 e 429 com `Retry-After`. O 502 deixa de existir |

O front já trata qualquer outro status com mensagem genérica, então não precisa de mudança de código.

## peca-backend

- [x] **T1** — criar `LimitadorPedidosSemConta`: guarda em memória os instantes por digest do email, com
  salt gerado na inicialização, e descarta o que sai da janela de 1 hora · `auth`, nível 2 · implementa
  `R-8`
- [x] **T2** — em `AuthService.exigirDentroDoLimite`, somar os pedidos em memória aos códigos gravados;
  registrar o pedido sem conta e sem `nome` depois dos limites, sem gravar no banco · `auth`, nível 2 ·
  implementa `R-3`, `R-8`, `R-9`, `AC-14`, `AC-15`, `AC-16`, `AC-19`
- [x] **T3** — criar o evento `CodigoOtpSolicitado`, sem expor o código em `toString`, e o executor limitado
  em `config/AsyncConfig` (`@EnableAsync`, sem rodar na thread da requisição; rejeição só vai para o log) ·
  `email`, `config`, nível 1 · implementa `R-11`
- [x] **T4** — criar o listener `AFTER_COMMIT` e assíncrono que chama `EmailService` e não relança a falha ·
  `email`, nível 1 · implementa `R-11`, `AC-11`, `AC-17`, `AC-18`
- [x] **T5** — trocar a chamada direta ao `EmailService` em `AuthService` pela publicação do evento ·
  `auth`, nível 2 · implementa `R-11`, `AC-17`, `AC-18`
- [x] **T6** — remover o handler de `EnvioEmailException` e o 502 do Swagger, ajustar a descrição do 200 e os
  comentários que afirmam o oposto · `exception`, `auth/AuthController` · implementa `R-3`, `R-11`
- [x] **T7** — testes unitários: `AuthServiceTest` (limites sem conta, combinação com e sem `nome`, nada
  gravado, evento publicado) e `LimitadorPedidosSemContaTest` (janela, chave sem email em texto puro,
  limpeza) · cobre `R-3`, `R-8`, `AC-14`, `AC-15`, `AC-16`, `AC-19`
- [x] **T8** — testes de integração em `SegurancaIntegrationTest`: mesma resposta com e sem conta, 200 com
  SMTP travado e com SMTP falhando, nenhum email após rollback · cobre `R-3`, `R-11`, `AC-11`, `AC-14`,
  `AC-17`, `AC-18`
- [x] **T8b** — teste de `R-11`: o log de falha no envio não leva o destinatário · `email`, `EmailServiceTest`
- [x] **T8c** — criar `TempoMinimoResposta` e usar em `AuthController.solicitarCodigo`, fora da transação para
  cobrir o commit · `auth` · implementa `R-3`, `AC-20`
- [x] **T8d** — testes: `TempoMinimoRespostaTest` e tempo mínimo de 200 e 429 em `SegurancaIntegrationTest` ·
  cobre `R-3`, `AC-20`
- [x] **T9** — rodar `./mvnw clean package` com `JAVA_HOME=/usr/lib/jvm/java-25-openjdk`
- [x] **T10** — atualizar `ROADMAP.md` com `[-]`, aguardando teste humano

## peca-front-end

Sem mudança de código.

- [ ] **T11** — validação manual, em tela de celular: em `/login`, pedir código para email sem conta, abrir
  "Criar conta" e conferir o aviso de espera do 429; pedir código com conta e conferir a chegada no Mailpit

## Docs

- [x] **T12** — tirar `D-1` e a parte resolvida de `D-8` de `rules.md`; atualizar P-1 em
  `docs/Architecture.md`, a frase sobre filas e workers em `ai/module_map.md` e a lista de status em
  `ai/conventions.md`
