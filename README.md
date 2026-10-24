# STREET PLAY — Launcher premium para Android TV

*Seu entretenimento. Do seu jeito.* · Kotlin · Jetpack Compose for TV · Room · DataStore · MVVM · **zero anúncios**

## Como gerar o APK instalável
O APK **não vem pré-compilado** (o ambiente onde o código foi escrito não tem Android SDK).

**Opção A — GitHub Actions (sem instalar nada):**
1. Crie um repositório e envie esta pasta (`git init && git add . && git commit -m init && git push`).
2. Aba **Actions → Build APK**. Ao terminar, baixe o artefato `StreetPlay-debug-apk`.

**Opção B — Android Studio:** abra a pasta, aguarde o sync, **Build → Build APK(s)**.
Saída: `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar na TV / TV Box
```
adb connect IP_DA_TV:5555
adb install -r app-debug.apk
```
Ou copie o APK num pendrive e abra com um gerenciador de arquivos. Ele aparecerá na lista de apps da TV.
Para torná-lo o launcher: *Configurações → Street Play → Definir como launcher* (depende do fabricante).
Release assinado: configure `signingConfigs` com seu keystore (nunca versionar o `.jks`).

## Arquitetura
```
app/src/main/java/br/com/streetplay/
├─ core/design     tokens, tema e componentes SP* (SPButton, SPAppCard, SPSidebar, SPDialog…)
├─ core/database   Room: favoritos e histórico de uso
├─ core/storage    DataStore: papel de parede, cor, animações
├─ core/common     intents do sistema, carregador de ícones, utilitários
├─ data / domain   AppRepository (PackageManager), catálogo dos 6 atalhos, modelos
├─ features/       home, applications, favorites, entertainment, store, settings, about
└─ navigation      AppShell + Destination (menu lateral)
```

## Decisões e limitações
- **Package visibility:** `<queries>` declara só intents de launcher + os pacotes dos 6 atalhos. Sem `QUERY_ALL_PACKAGES`.
- **Recentes:** o Android exige permissão especial (Usage Access) para ler uso de outros apps. Alternativa real: o Street Play registra localmente os apps abertos *por ele*.
- **Papel de parede:** 4 presets. Imagem própria exigiria seletor de arquivos, ausente em muitas TVs.
- **Ícones do menu:** numerados (fonte-safe). Ícones vetoriais ficam para a v1.1.
- **Launcher padrão:** só onde o fabricante permitir; o app abre a tela do sistema, não força nada.
- **Sem** AdMob/SDKs de anúncio, Firebase, Supabase, INTERNET, rastreadores ou APIs privadas.

## Status de verificação
Código escrito mas **ainda não compilado nem testado em dispositivo**. Rode a pipeline (testes unitários + build) antes de tudo; corrija eventuais ajustes de API do `tv-material` se o compilador apontar.
