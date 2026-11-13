# Publicação na Google Play Store

Guia completo do que foi ajustado no projeto e o que preencher no
**Play Console**. Tudo que é automatizável já está pronto; o restante depende da
sua conta de desenvolvedor.

---

## 1. O que já está adequado no projeto

| Requisito da Play Store | Status |
|---|---|
| `targetSdk` 36 (obrigatório para novos apps desde ago/2026) | ✅ `targetSdk = 36` / `compileSdk = 36` |
| Android App Bundle (`.aab`) para apps novos | ✅ `./gradlew bundleRelease` |
| Assinatura de release | ✅ keystore local (vira *upload key*) |
| `versionCode`/`versionName` crescentes | ✅ `2` / `1.1.0` |
| `android:exported` explícito nas activities | ✅ |
| Backup desativado (vídeos grandes/temporários) | ✅ `allowBackup="false"` |
| Callback de gesto "voltar" do Android 13+ | ✅ `enableOnBackInvokedCallback="true"` |
| Categoria de app declarada | ✅ `appCategory="video"` |
| Sem permissões perigosas | ✅ nenhuma permissão no manifesto |
| Sem permissão de internet | ✅ app 100% offline |
| `queries` para detectar WhatsApp (Android 11+) | ✅ |
| Alinhamento de página de 16 KB (Android 15+) | ✅ verificado com `zipalign -c -P 16` |
| Lint falhando o build em erros | ✅ `lint.abortOnError = true` |
| Política de privacidade | ✅ [`PRIVACY.md`](../PRIVACY.md) |
| Licença | ✅ [`LICENSE`](../LICENSE) (MIT) |
| Ícone 512×512, feature graphic 1024×500, screenshots | ✅ `store/` |

> **Importante:** o `versionCode` precisa ser maior que o da versão já publicada.
> Sempre incremente antes de um novo envio.

---

## 2. Gerar o AAB assinado

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew clean bundleRelease
```

Saída:

```
app/build/outputs/bundle/release/app-release.aab
```

O script `scripts/build-release.sh` faz isso e ainda copia os artefatos para
`dist/`.

Verificar a assinatura do AAB:

```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab | tail -5
```

### Play App Signing

No primeiro envio, o Play Console oferece o **Play App Signing** (obrigatório
para apps novos). O keystore `keystore/divide30s-release.jks` passa a ser a
**upload key** — é ele que assina o AAB enviado. O Google guarda a chave final
de assinatura.

- Se o Play gerar a chave: use a upload key existente (`keystore.properties` já
  configurado).
- Depois de criar o app, copie o **SHA-256 do certificado de assinatura do app**
  exibido em *Configuração → Integridade do app* e guarde junto do keystore.
- Se perder a upload key, é possível solicitar o reset dela no Console; a chave
  de assinatura do app **não** pode ser perdida.

### Identificadores deste build

| Item | Valor |
|---|---|
| `applicationId` | `com.cleber.videosplit30` |
| `versionCode` | `5` |
| `versionName` | `1.2.1` |
| `minSdk` / `targetSdk` / `compileSdk` | `26` / `36` / `36` |
| SHA-256 do APK | `56d800cc44efb2ba2e5faf1092a117b96fa2f43c64653e0e4f58ea192c20cd44` |
| SHA-256 do AAB | `830a07ef19b5c6c2003d02c6d1b8ea20cbdf4b689a7ad5605ce3490f609b2914` |

---

## 3. Ficha da loja (textos prontos)

### Nome do app (máx. 30 caracteres)

```
Divide 30s — vídeos em partes
```

### Descrição curta (máx. 80 caracteres)

```
Divida vídeos em partes de 30s sem perder qualidade e envie tudo de uma vez.
```

### Descrição completa (máx. 4000 caracteres)

```
O Divide 30s corta seus vídeos em partes de aproximadamente 30 segundos SEM reencodificar. Isso significa que a qualidade, a resolução, o áudio e os metadados originais são preservados exatamente como estavam — o app apenas copia os dados já comprimidos para novos arquivos.

É a ferramenta ideal para quem precisa dividir vídeos longos para o WhatsApp, para stories, para enviar por e-mail ou para contornar limites de tamanho de arquivo.

PRINCIPAIS RECURSOS

• Divisão sem perda de qualidade: nenhum decoder ou encoder é usado, então o vídeo não é recompactado e não perde nitidez.
• Rápido: como não há recompressão, o tempo de processamento é muito menor do que o de um editor tradicional.
• Compartilhamento em lote: depois de dividir, envie TODAS as partes de uma vez para o WhatsApp ou para qualquer outro app.
• Modo documento: envie as partes como documento para o WhatsApp não recomprimir o vídeo.
• Barra de progresso e cancelamento a qualquer momento.
• Verificação de espaço livre antes de começar.
• Limpeza automática das partes anteriores para não ocupar espaço à toa.

COMO USAR

1. Toque em "Escolher vídeo" e selecione o arquivo.
2. Confira a duração e o tamanho e toque em "Dividir em partes de 30s".
3. Escolha "Compartilhar no WhatsApp", "Enviar como documento" ou "Compartilhar em outro app".

SOBRE O CORTE

O corte é feito no quadro-chave (keyframe) mais próximo depois dos 30 segundos. Por isso cada parte costuma ter entre 30 e 32 segundos. Esse é justamente o motivo de não haver perda de qualidade: cortar no milissegundo exato exigiria reencodar o vídeo.

PRIVACIDADE

O app não tem permissão de internet, não coleta dados, não mostra anúncios e não envia nada para servidores. Todo o processamento acontece no seu aparelho. Você pode conferir o código-fonte e a política de privacidade no repositório do projeto.

REQUISITOS

Android 8.0 (API 26) ou superior. Formatos mais comuns de celular (MP4 com H.264/HEVC + AAC) são totalmente suportados; WebM/VP9 requer Android 10 ou superior.

Código-fonte: https://github.com/clebersys/divide30s
```

### Categoria e tags

- **Categoria:** Vídeo players e editores
- **Tags sugeridas:** edição de vídeo, utilitários, compartilhamento
- **Tipo de app:** Aplicativo (não é jogo)

### Detalhes de contato

- **E-mail:** use um e-mail que você acessa (obrigatório e público).
- **Site:** `https://github.com/clebersys/divide30s`
- **Política de privacidade:**
  `https://github.com/clebersys/divide30s/blob/main/PRIVACY.md`

> A URL da política precisa ser pública e abrir sem login — o link acima atende.

---

## 4. Recursos gráficos (pasta `store/`)

| Recurso | Especificação | Arquivo |
|---|---|---|
| Ícone do app | PNG 512×512, 32 bits, sem transparência | `store/icon-512.png` |
| Feature graphic | PNG/JPEG 1024×500, sem transparência | `store/feature-graphic-1024x500.png` |
| Screenshot 1 | 1080×2400 (9:16) | `store/screenshot-1.png` |
| Screenshot 2 | 1080×2400 (9:16) | `store/screenshot-2.png` |
| Screenshot 3 | 1080×2400 (9:16) | `store/screenshot-3.png` |

Requisitos oficiais dos screenshots: mínimo 2, máximo 8, JPEG ou PNG de 24
bits, entre 320 px e 3840 px no maior lado, proporção entre 16:9 e 9:16.

> Os arquivos desta pasta já estão normalizados: screenshots em **PNG 24 bits**
> (sem canal alfa), `1080×1920` (9:16 exato), ícone em **PNG 32 bits** e feature
> graphic em 24 bits. Capturados em emulador **Android 16 (API 36)**.

> Se mudar a interface, recapture com:
> `adb exec-out screencap -p > store/screenshot-N.png` e depois rode
> `python3 scripts/make-store-assets.py` para normalizar o formato.

---

## 5. Data safety (Segurança dos dados)

Preencha exatamente assim:

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha algum tipo de dado obrigatório do usuário? | **Não** |
| Os dados são criptografados em trânsito? | Não se aplica (nenhum dado sai do aparelho) |
| Você oferece uma forma de excluir os dados? | Não se aplica (nada é coletado) |
| Dados coletados por SDKs de terceiros? | **Nenhum** — o app não usa SDK de analytics, ads ou crash reporting |
| Dados de saúde, financeiros, mensagens, fotos, arquivos? | **Nenhum** é coletado ou transmitido |

Justificativa técnica: o `AndroidManifest.xml` não declara `INTERNET`, não há
bibliotecas de terceiros e os vídeos nunca saem do aparelho sem uma ação
explícita de compartilhamento.

---

## 6. Classificação de conteúdo (questionário IARC)

Responda:

| Pergunta | Resposta |
|---|---|
| Categoria do app | Utilitário / produtividade / comunicação / outro |
| Violência, sangue, conteúdo sexual, nudez | Não |
| Linguagem imprópria | Não |
| Drogas, álcool, tabaco | Não |
| Jogos de azar, apostas | Não |
| Interação entre usuários / conteúdo gerado por usuários | Não |
| Compartilhamento de localização | Não |
| Compras digitais | Não |
| Anúncios | **Não** |
| Permite compras? | Não |
| Coleta dados pessoais? | Não |

Classificação esperada: **Livre / Everyone**.

---

## 7. Outras seções do Play Console

| Seção | Como preencher |
|---|---|
| **Acesso ao app** | "Todas as funcionalidades estão disponíveis sem login" |
| **Anúncios** | O app não contém anúncios |
| **Público-alvo e conteúdo** | 13+ (o app não é destinado a crianças) |
| **Apps governamentais** | Não |
| **App de notícias** | Não |
| **Rastreamento de contato COVID-19** | Não |
| **Recursos de saúde** | Não |
| **Recursos financeiros** | Não |
| **Permissões sensíveis** | Nenhuma declarada — nada a justificar |
| **Publicidade de app** (ads declaration) | Não |
| **Teste interno** | Recomendado antes do lançamento (ver seção 9) |

---

## 8. Checklist de envio

1. [ ] Criar o app no Play Console (nome, idioma padrão `pt-BR`, app gratuito).
2. [ ] Ativar o **Play App Signing**.
3. [ ] Preencher ficha da loja (textos da seção 3 + gráficos da seção 4).
4. [ ] Preencher **Segurança dos dados** (seção 5).
5. [ ] Responder o **questionário de classificação** (seção 6).
6. [ ] Configurar **Acesso ao app**, **Público-alvo**, **Anúncios** (seção 7).
7. [ ] Informar a **URL da política de privacidade**.
8. [ ] Enviar o `app-release.aab` em **Teste interno** primeiro.
9. [ ] Instalar pelo link do teste interno e validar divisão + compartilhamento.
10. [ ] Promover para **Produção** e enviar para revisão.

### Observações importantes

- **Conta de desenvolvedor:** a partir de 2023, contas pessoais novas precisam de
  um teste fechado com pelo menos **12 testadores por 14 dias** antes de publicar
  em produção. Contas de organização não têm essa exigência.
- **Revisão:** a primeira análise costuma levar de algumas horas a alguns dias.
- **Atualizações futuras:** sempre incremente `versionCode` no
  `app/build.gradle.kts` antes de gerar um novo AAB.
- **Rejeições comuns neste tipo de app:** declarar coleta de dados que não
  existe, esquecer a política de privacidade ou alegar permissões que o app não
  usa. Este projeto já está alinhado.

---

## 9. Notas de versão sugeridas (v1.2.1)

```
Novidades desta versão.

• Tela de pré-visualização: ao escolher o vídeo, toque para dar play e conferir se é o arquivo correto antes de dividir.
• Mantém a divisão em partes de ~30s sem reencodificar (qualidade original).
• Compartilha todas as partes de uma vez no WhatsApp (inclui modo documento).
• As partes da divisão anterior são apagadas ao escolher outro vídeo.
• Compatível com Android 8.0+ e alinhado ao targetSdk 36.
```
