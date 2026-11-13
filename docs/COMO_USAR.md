# Como usar

## Instalar

1. Copie `Divide30s-1.1.0-release.apk` para o celular (cabo, Drive, Telegram etc.).
2. Abra o arquivo e permita **"Instalar apps desconhecidos"** para o app usado
   na abertura (Arquivos, Chrome etc.).
3. Confirme a instalação.

Via ADB:

```bash
adb install "dist/Divide30s-1.1.0-release.apk"
```

O app não pede nenhuma permissão de câmera, microfone, contatos ou
armazenamento.

## Dividir um vídeo

1. Abra o **Divide 30s**.
2. Toque em **Escolher vídeo**.
3. Selecione o vídeo no seletor do Android.
4. Confira as informações e toque em **Dividir em partes de 30s**.
5. Aguarde a barra de progresso. Você pode cancelar a qualquer momento.

O tempo varia com o tamanho do vídeo, mas costuma ser bem rápido porque nada é
reencodado — é só cópia de dados.

## Compartilhar no WhatsApp

Na tela de resultado você tem três opções:

| Botão | O que faz |
|---|---|
| **Compartilhar no WhatsApp** | Abre o WhatsApp com **todas as partes** anexadas de uma vez. |
| **Enviar como documento (sem compressão)** | Envia como documento; o WhatsApp não recomprime e o arquivo chega idêntico. |
| **Compartilhar em outro app** | Abre o seletor do Android (Drive, Telegram, e-mail etc.). |

### Por que às vezes o WhatsApp comprime?

Quando você envia um vídeo como **mídia**, o WhatsApp reencoda para reduzir o
tamanho. Para manter a qualidade original, use **"Enviar como documento (sem
compressão)"**. Nesse modo o destinatário recebe o MP4 original.

### Limite de arquivos

O WhatsApp pode limitar quantos vídeos aceita por envio (varia por versão). Se
ele recusar o grupo inteiro:

- envie em grupos menores;
- use o modo documento;
- ou compartilhe por outro app.

## Onde ficam os arquivos

```
Android/data/com.cleber.videosplit30/files/Movies/Divide30s/<data_hora>/
```

Essa pasta é privada do app; os arquivos **não aparecem na galeria**. Ao iniciar
uma nova divisão, as partes anteriores são apagadas automaticamente para não
acumular vídeos.

Para guardar uma parte permanentemente, compartilhe com o Drive/Arquivos ou
mova o arquivo pelo gerenciador de arquivos.

## Perguntas frequentes

**O vídeo ficou com 31s, não 30s. É normal?**
Sim. O corte é feito no keyframe mais próximo depois dos 30s para não reencodar.
Dependendo do GOP, a parte fica entre 30 e 32s (ou mais, se o GOP for longo).

**A qualidade mudou?**
Não. Os samples comprimidos são copiados; não há decoder/encoder no caminho.

**Por que o app não abre vídeos do WhatsApp?**
Vídeos recebidos podem estar em formatos/containers não suportados pelo
`MediaMuxer` do aparelho. Converta para MP4/H.264 ou teste em outro aparelho.

**Apareceu "Espaço insuficiente".**
A divisão cria uma cópia completa do vídeo. Libere espaço equivalente ao
tamanho do arquivo original (mais uma margem de 100 MB).

**O compartilhamento não abre o WhatsApp.**
Verifique se o WhatsApp está instalado. Se estiver, o app cai no seletor padrão
do Android; escolha o WhatsApp manualmente.

**Posso dividir vídeos com mais de 1 hora?**
Sim, mas o resultado terá muitas partes (uma a cada ~30s) e ocupará o mesmo
espaço do original. O limite interno é de 5.000 partes.

**Perdi o keystore. E agora?**
Sem o keystore original não é possível atualizar o app já instalado; será
preciso desinstalar e instalar uma nova assinatura. Guarde
`keystore/divide30s-release.jks` e `keystore.properties` em local seguro.
