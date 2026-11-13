# Divide 30s

Aplicativo Android que divide vídeos em partes de **30 segundos sem reencodar**
(sem perder qualidade) e compartilha **todas as partes de uma vez** pelo WhatsApp.

![Tela inicial](docs/screenshot-inicial.png)

## Funcionalidades

- 🎬 Divide qualquer vídeo suportado pelo Android em partes de ~30s.
- 🔒 **Sem re-encode**: os samples comprimidos são copiados, a qualidade é a
  original e o processamento é rápido.
- 📤 Compartilha o grupo inteiro de uma vez (`ACTION_SEND_MULTIPLE`).
- 🟢 Atalho direto para o WhatsApp, com fallback para o seletor do sistema.
- 📄 Opção **"Enviar como documento (sem compressão)"** para o WhatsApp não
  recomprimir os vídeos.
- 📊 Progresso em tempo real e cancelamento.
- 💾 Checagem de espaço livre antes de começar.
- 🧹 Limpa automaticamente as partes da divisão anterior.
- 🌗 Interface em Jetpack Compose, tema escuro.

## Como funciona

```mermaid
flowchart LR
    A[Vídeo original] --> B[MediaExtractor]
    B --> C{Sample é keyframe<br/>e passou de 30s?}
    C -- Não --> D[MediaMuxer grava<br/>na parte atual]
    C -- Sim --> E[Fecha a parte<br/>e abre a próxima]
    D --> F[MP4 parte 001]
    E --> F
    F --> G[Compartilhar todas<br/>de uma vez]
```

1. O vídeo é aberto com `MediaExtractor`, que entrega os samples **já
   comprimidos** (H.264/HEVC + AAC etc.).
2. Cada sample é escrito em um `MediaMuxer` novo, sem passar por decoder/encoder.
3. Quando aparece um **keyframe** depois dos 30s, a parte atual é fechada e a
   próxima começa exatamente nesse keyframe.
4. No fim, todos os arquivos são compartilhados juntos via `FileProvider` +
   `Intent.ACTION_SEND_MULTIPLE`.

> O corte é feito no keyframe mais próximo depois dos 30s. Por isso as partes
> ficam com **30–32s** (dependendo do GOP do vídeo). Cortar no milissegundo
> exato exigiria reencodar e perderia qualidade — o que contraria o objetivo.

## Requisitos

- Android **8.0 (API 26)** ou superior.
- Para compilar: JDK 17+ e Android SDK 35 (Gradle wrapper incluso).

## Instalação

APK assinado pronto:

```
dist/Divide30s-1.0-release.apk
```

Via ADB:

```bash
adb install "dist/Divide30s-1.0-release.apk"
```

Ou copie o APK para o celular e instale permitindo "fontes desconhecidas".

> O APK de release tem assinatura diferente do debug. Se você instalou o debug,
> desinstale antes: `adb uninstall com.cleber.videosplit30`.

## Uso

1. Abra o **Divide 30s**.
2. Toque em **Escolher vídeo** e selecione o vídeo.
3. Confira nome, duração e tamanho e toque em **Dividir em partes de 30s**.
4. Ao terminar, escolha:
   - **Compartilhar no WhatsApp** — envia todas as partes de uma vez;
   - **Enviar como documento (sem compressão)** — mantém o arquivo original
     (o WhatsApp não recomprime);
   - **Compartilhar em outro app** — abre o seletor do Android.

Guia detalhado: [docs/COMO_USAR.md](docs/COMO_USAR.md)

## Build

```bash
# debug
./gradlew assembleDebug

# release assinado (usa keystore.properties)
./gradlew assembleRelease
```

Saídas:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### Assinatura

O release usa o keystore `keystore/divide30s-release.jks` (alias `divide30s`).
As senhas ficam em `keystore.properties`, ambos fora do Git.

> ⚠️ Guarde o keystore **e** o `keystore.properties` em local seguro. Sem eles
> não é possível atualizar o app já instalado — só reinstalar como app novo.

Certificado atual (SHA-256):

```
2C:86:DC:45:A8:B4:C2:1A:F1:13:D2:62:5F:2F:CF:98:8F:67:A4:BC:09:04:7D:FE:C6:56:A1:EA:B0:5F:D4:C4
```

## Testes

Teste instrumentado que divide um vídeo real de 65s e valida quantidade,
duração, resolução e legibilidade das partes:

```bash
./gradlew connectedDebugAndroidTest
```

Resultado da última execução (emulador Android 15):

- 65s → **3 partes** (0–30s, 30–60s, 60–65s)
- todas abrem como vídeo válido, mesma resolução (320×240) e duração esperada
- soma dos tamanhos ≈ arquivo original (confirma ausência de re-encode)
- `lintDebug` sem erros

## Estrutura

```
app/src/main/java/com/cleber/videosplit30/
├── MainActivity.kt      # UI Compose + navegação de estados
├── SplitViewModel.kt    # estado, metadados, progresso, espaço em disco
├── VideoSplitter.kt     # divisão lossless com MediaExtractor/MediaMuxer
└── ShareUtils.kt        # Intents de compartilhamento + FileProvider

app/src/androidTest/     # teste instrumentado com vídeo de amostra
app/src/main/res/xml/file_paths.xml
docs/                    # arquitetura e guia de uso
```

Detalhes técnicos: [docs/ARQUITETURA.md](docs/ARQUITETURA.md)

## Limitações

- **Corte em keyframe**: partes de 30–32s, não 30,000s exatos.
- **Containers**: MP4/H.264/HEVC (padrão de celular). WebM/VP9 requer Android 10+.
- **WhatsApp**: recomprime vídeos enviados como mídia; use o modo documento para
  manter o original. O app também pode esbarrar no limite de arquivos por envio
  do WhatsApp.
- **Armazenamento**: as partes ficam na pasta privada do app
  (`Android/data/com.cleber.videosplit30/files/Movies/Divide30s/`) e não
  aparecem na galeria; são apagadas ao iniciar uma nova divisão.
- **Assinatura**: este repositório não inclui o keystore nem as senhas.

## Privacidade

O app não usa internet, não coleta dados e não envia nada para servidores. Todo o
processamento é local. O compartilhamento só acontece quando você toca em um dos
botões e escolhe o app de destino.

## Licença

Uso pessoal. Defina uma licença antes de distribuir publicamente.
