# Product — PEÇA

## O que é

O PEÇA é um marketplace em que técnicos de celular anunciam e procuram peças de reposição, como
telas, baterias e flex. O anúncio mostra foto, preço, modelo compatível e o WhatsApp do vendedor. A
negociação e o pagamento acontecem fora da plataforma, pelo WhatsApp. O login não usa senha: a
pessoa recebe por email um código de 6 dígitos.

## Atores

- **Visitante**: navega pela vitrine, busca peças, vê detalhes e chama o vendedor no WhatsApp, sem
  login.
- **Técnico (usuário autenticado)**: tudo o que o visitante faz, mais criar, editar e excluir os
  próprios anúncios e editar o próprio perfil. Quem anuncia aparece publicamente como vendedor, com
  nome e WhatsApp.
- Não existe papel de administrador nem moderação.

## Escopo

Na versão atual:

- Cadastro e login por OTP, com JWT.
- Perfil com nome e WhatsApp; o email não pode ser alterado.
- Anúncios com uma foto, categoria e modelo de celular.
- Vitrine pública, busca, filtro por categoria e detalhe da peça.
- Contato pelo WhatsApp.

Fora do escopo:

- Pagamento, carrinho e frete dentro da plataforma.
- Chat próprio.
- Avaliações de vendedores.
- Área administrativa.
- Senhas e login social.

## Roadmap

Itens pendentes nos `ROADMAP.md` dos dois repositórios:

- Troca de email com confirmação por OTP no novo endereço.
- Endpoint próprio para "Meus anúncios".
- Galeria com várias fotos por peça.
- Vários modelos compatíveis por peça.
- Campo de condição da peça (Nova, Original, Retirada, Usada).
- Cidade do anunciante e selo de técnico verificado.
- Data do anúncio.
- Frete (grátis ou retirada) como informação do anúncio.

## Ambientes e restrições

- Tudo roda numa máquina só com `docker compose up --build`. O único serviço externo opcional é um
  SMTP real; sem ele, os emails ficam no Mailpit.
- Público brasileiro: interface em pt-BR e preços em reais. O desenvolvimento é mobile first.
- Os dados pessoais seguem a LGPD. Nome e WhatsApp do vendedor são públicos; o email não.
