# Política de Privacidade — Divide 30s

**Última atualização: 13 de novembro de 2026**

O **Divide 30s** (`com.cleber.videosplit30`) é um aplicativo Android que divide
vídeos em partes de aproximadamente 30 segundos, sem reencodificar, e permite
compartilhar as partes por outros aplicativos — principalmente o WhatsApp.

## Resumo

**O app não coleta, não transmite e não armazena dados pessoais em servidores.**
Todo o processamento acontece no seu aparelho.

## Quais dados o app acessa

| Dado | Para quê | Onde fica | Enviado para fora? |
|---|---|---|---|
| Vídeo escolhido por você | Ler os samples e gerar as partes | Somente no seu aparelho | Não |
| Nome, duração e tamanho do vídeo | Mostrar a tela de confirmação e estimar o espaço necessário | Memória do app (não persistido) | Não |
| Arquivos gerados (partes) | Compartilhar quando você toca em um botão | Pasta privada do app | Apenas para o app que você escolher no compartilhamento |
| Espaço livre em disco | Avisar sobre espaço insuficiente | Não é armazenado | Não |

## O que o app NÃO faz

- Não pede acesso à internet — o manifesto **não declara a permissão
  `INTERNET`**.
- Não coleta identificadores (IMEI, Android ID, Advertising ID, e-mail, telefone).
- Não usa analytics, crash reporting, SDK de anúncios ou rastreamento.
- Não cria conta, não pede login e não sincroniza nada.
- Não lê contatos, câmera, microfone, localização ou histórico.
- Não grava os vídeos na galeria nem os envia para servidores próprios.

## Permissões declaradas

O app **não declara nenhuma permissão perigosa**. O acesso ao vídeo acontece pelo
seletor do sistema (Storage Access Framework), que concede leitura apenas ao
arquivo que você escolher. A leitura temporária dos arquivos gerados por outros
apps é concedida por `FileProvider` no momento do compartilhamento.

## Compartilhamento com terceiros

Ao tocar em **Compartilhar no WhatsApp**, **Enviar como documento** ou
**Compartilhar em outro app**, o Android abre o app de destino escolhido por
você e entrega os arquivos selecionados. A partir desse momento, o tratamento
dos dados passa a ser responsabilidade do aplicativo escolhido (por exemplo, a
política de privacidade do WhatsApp). O Divide 30s não tem controle nem acesso
ao que o app de destino faz com os arquivos.

## Armazenamento e exclusão

As partes geradas ficam em uma pasta privada do app:

```
Android/data/com.cleber.videosplit30/files/Movies/Divide30s/<data_hora>/
```

Esses arquivos são apagados automaticamente quando você inicia uma nova divisão.
Para removê-los imediatamente, use **Configurações → Apps → Divide 30s →
Armazenamento → Limpar dados**, ou desinstale o app. Nenhum dado permanece após
a desinstalação.

## Crianças

O app não é direcionado a crianças menores de 13 anos e não coleta dados de
nenhum usuário, incluindo crianças.

## Alterações nesta política

Mudanças serão publicadas neste mesmo arquivo no repositório
<https://github.com/clebersys/divide30s>. A data no topo indica a última
revisão.

## Contato

Dúvidas sobre privacidade: abra uma issue em
<https://github.com/clebersys/divide30s/issues> ou escreva para
**clebersys@users.noreply.github.com**.

---

# Privacy Policy — Divide 30s

**Last updated: November 13, 2026**

**Divide 30s** splits videos into ~30-second parts without re-encoding and lets
you share those parts through other apps, mainly WhatsApp.

**The app does not collect, transmit, or store personal data on any server.**
All processing happens on your device.

- The app does **not** declare the `INTERNET` permission.
- No analytics, crash reporting, ads, tracking, accounts, or identifiers.
- No contacts, camera, microphone, location, or storage permissions.
- The selected video is opened through the system picker (SAF) and read-only.
- Generated parts live in the app's private folder and are deleted when a new
  split starts, when you clear app data, or when you uninstall the app.
- When you tap a share button, files are handed to the app you choose; from then
  on that app's privacy policy applies.

Contact: <https://github.com/clebersys/divide30s/issues>
