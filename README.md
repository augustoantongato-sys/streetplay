# STREET PLAY

*Seu entretenimento. Do seu jeito.*

## Site oficial

O arquivo `index.html` contém a central de downloads. O projeto pode ser publicado como site estático na Vercel usando a raiz deste repositório.

Domínio desejado: **https://streetplay.vercel.app**

Para publicar:
1. Importe este repositório na Vercel.
2. Selecione a branch `test-chatgpt-access-20261026-2` durante a validação.
3. Use a raiz do repositório como diretório do projeto; não é necessário comando de build para a página estática.
4. Em Settings → Domains, associe `streetplay.vercel.app` ao projeto e conclua a validação DNS solicitada pela Vercel.

A página consulta a última Release pública do GitHub e só libera os downloads quando encontra arquivos com nomes compatíveis:
- `StreetPlay-TV.apk`
- `StreetPlay-Mobile.apk`
- um instalador `.exe`

Até que esses arquivos sejam publicados, os botões ficam desativados. Isso evita oferecer downloads inexistentes.

## Aplicativo Android

Projeto nativo Kotlin + Jetpack Compose, com suporte a Android TV e dispositivos Android com tela sensível ao toque. O APK atual é um pacote Android universal; ainda não são dois APKs independentes.

### Gerar APK

A pipeline GitHub Actions executa testes unitários e `:app:assembleDebug`. O artefato atual é publicado como `StreetPlay-debug`.

Último build validado:
- Run: https://github.com/augustoantongato-sys/streetplay/actions/runs/40367674828
- Commit: `ee9edab266bfeaccdffe0676b8ec57266210db66`
- Testes unitários: sucesso
- Build APK: sucesso

## Experiência de filmes e séries

A tela IPTV usa os dados reais retornados pela conta Xtream cadastrada:
- Filmes: destaque, catálogo em fileira, detalhes e reprodução no player interno.
- Séries: catálogo, detalhes, temporadas e episódios.
- A disponibilidade de conteúdos depende do provedor e da conta do usuário.

## Plataformas e estado atual

| Plataforma | Estado |
|---|---|
| Android TV | APK Android universal compila; validar navegação em dispositivo real |
| Celular Android | APK Android universal compila; adaptar e validar layout touch em dispositivo real |
| Windows | Aplicativo instalável ainda precisa ser desenvolvido e compilado |
| Site | Página estática pronta para publicação na Vercel; domínio precisa ser associado ao projeto |

## Importante

O Street Play é um player e interface de acesso. O usuário deve fornecer uma conta de provedor IPTV e ter autorização para acessar os conteúdos. Não são fornecidos canais, filmes ou séries pelo projeto.
