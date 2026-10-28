# RingPrint

**Troque o ícone da digital na tela de bloqueio e no AOD da One UI por um aro minimalista (estilo HyperOS) ou por um ícone seu.**

Módulo LSPosed (API legada do Xposed) que **não modifica nenhum APK do sistema**. Para reverter, basta desativar o módulo.

[English version below](#english)

---

## Como funciona

O ícone da digital da Samsung não é uma imagem: são animações **Lottie (JSON)** dentro do app de biometria (`com.samsung.android.biometrics.app.setting`). Esse app tem `sharedUserId="android.uid.system"` e é assinado com a chave da Samsung, então **não dá para reassinar o APK** (o sistema recusa e o sensor para de funcionar).

O RingPrint resolve isso por hook. Dentro do processo do app de biometria, ele intercepta `AssetManager.open()` e, quando o app pede um `ic_fingerprint_*.json`, entrega o aro (ou o ícone importado) no lugar. O APK original fica intacto.

Arquivos interceptados:

| Arquivo | Onde aparece |
|---|---|
| `ic_fingerprint_aod.json` | AOD |
| `ic_fingerprint_dark_theme.json` / `_non_alpha` | Tela de bloqueio, tema escuro |
| `ic_fingerprint_light_theme.json` / `_non_alpha` | Tela de bloqueio, tema claro |

## Recursos

- Aro minimalista por padrão, no estilo HyperOS.
- Tela de configuração com **diâmetro** e **espessura**, com prévia ao vivo.
- **Importar ícone próprio**:
  - **PNG** com fundo transparente (redimensionado automaticamente e controlado pela barra de diâmetro).
  - **Lottie `.json`** (usado como vem; arquivos `.lottie` não são suportados).
  - Espaços separados para o tema **escuro/AOD** e o tema **claro**.
- Botão **Aplicar**, que reinicia só o processo da digital via root (não precisa reiniciar o celular).
- Botão **Reiniciar o celular**, com confirmação.

## Requisitos

- Android com **root** e **LSPosed** (testado com KernelSU).
- Samsung **One UI**. Testado na **One UI 8 (Android 16)** em um **Galaxy S20 FE**.
- Para compilar: Android Studio, **JDK 17 a 21** (não use o JDK 25), Gradle 8.7, plugin Android Gradle 8.5.2, `compileSdk 34`.

> Outros aparelhos e versões da One UI não foram testados. Se o ícone da sua versão estiver em outro app ou usar outro caminho de leitura, o hook pode não pegar.

## Instalação

1. Compile o projeto (veja abaixo) ou baixe o APK em **Releases**.
2. Instale o APK no celular.
3. Abra o **LSPosed** > Módulos > **RingPrint** e ative.
4. Marque apenas o escopo **`com.samsung.android.biometrics.app.setting`**.
5. Reinicie o celular (ou reinicie o processo do app de biometria).
6. Abra o app RingPrint, ajuste o aro ou importe um ícone e toque em **Aplicar**. Conceda a permissão de root quando pedir.

Para confirmar que o hook carregou, procure no log do LSPosed (aba do módulo):

```
FpRing: hook ativo em com.samsung.android.biometrics.app.setting
```

## Compilando

1. Abra a pasta do projeto no Android Studio e aguarde o Gradle sincronizar.
2. Em **Settings > Build Tools > Gradle**, escolha um JDK 17 a 21 (por exemplo o `jbr-21` embutido).
3. **Build > Build APK(s)**.
4. O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

A biblioteca do Xposed (`de.robv.android.xposed:api:82`) é baixada de `https://api.xposed.info/`.

## Dicas para ícones personalizados

- Use um ícone **branco** no tema escuro/AOD e um **escuro** no tema claro. Um ícone de uma cor só pode sumir num dos dois fundos.
- Prefira traços simples e limpos. O ícone é pequeno e fica semitransparente.
- Mantenha o desenho centralizado e do tamanho do sensor, para não confundir onde tocar.
- Lottie importado é usado sem alterações: as barras de diâmetro e espessura não valem para ele. Use uma tela de 288×288, como o original.

## Reverter / solução de problemas

- **Voltar ao normal:** desative o módulo no LSPosed e reinicie.
- **Ícone não mudou:** confira o escopo no LSPosed, veja se a linha `FpRing: hook ativo` aparece no log e toque em **Aplicar** de novo.
- **Aplicar não faz nada:** confirme que o RingPrint tem permissão de root no gerenciador (KernelSU, Magisk etc.).

## Aviso

O módulo atua dentro de um app do sistema. Use por sua conta e risco e deixe um jeito de desativar o módulo à mão (recovery ou `adb`). Este projeto não é afiliado à Samsung.

## Estrutura

```
app/src/main/java/com/fpring/lsp/
├── Hook.java            # hook em AssetManager.open no app de biometria
├── Icons.java           # Lottie do aro e gerador de Lottie a partir de PNG
├── ConfigProvider.java  # entrega configuração e ícones ao hook (outro processo)
└── MainActivity.java    # tela de configuração, importação e root
```

---

## English

# RingPrint

**Replace the fingerprint icon on the One UI lock screen and AOD with a minimal HyperOS-style ring, or with your own icon.**

An LSPosed module (legacy Xposed API) that **does not modify any system APK**. To revert, just disable the module.

### How it works

Samsung's fingerprint icon is not a bitmap: it is a set of **Lottie (JSON) animations** inside the biometrics app (`com.samsung.android.biometrics.app.setting`). That app uses `sharedUserId="android.uid.system"` and is signed with Samsung's key, so **re-signing the APK is not possible** (the system rejects it and the fingerprint sensor stops working).

RingPrint hooks `AssetManager.open()` inside that app's process. When the app requests an `ic_fingerprint_*.json`, the module returns a ring (or your imported icon) instead. The original APK stays untouched.

### Features

- Minimal ring by default, HyperOS style.
- Settings screen with **diameter** and **stroke width**, and a live preview.
- **Import your own icon**: transparent **PNG** (auto-resized, sized by the diameter slider) or **Lottie `.json`** (used as-is). Separate slots for **dark/AOD** and **light** themes.
- **Apply** button that restarts only the fingerprint process through root (no reboot needed).

### Requirements

- Rooted Android with **LSPosed** (tested with KernelSU).
- Samsung **One UI**. Tested on **One UI 8 (Android 16)** on a **Galaxy S20 FE**.
- To build: Android Studio, **JDK 17-21** (not JDK 25), Gradle 8.7, Android Gradle Plugin 8.5.2, `compileSdk 34`.

### Install

1. Build the project or download the APK from **Releases**, then install it.
2. In **LSPosed**, enable the module with the scope **`com.samsung.android.biometrics.app.setting`** only.
3. Reboot (or restart the biometrics process).
4. Open the app, adjust the ring or import an icon, then tap **Apply** and grant root.

### Disclaimer

The module runs inside a system app. Use at your own risk and keep a way to disable it (recovery or `adb`). Not affiliated with Samsung.
